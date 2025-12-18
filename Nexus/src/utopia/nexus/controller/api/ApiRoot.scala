package utopia.nexus.controller.api

import utopia.access.model.Headers
import utopia.access.model.enumeration.Method.Get
import utopia.access.model.enumeration.Status._
import utopia.access.model.enumeration.{Method, Status}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq.OptimizedSeqBuilder
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.string.Regex
import utopia.flow.util.result.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.nexus.controller.api.interceptor.{InterceptRequest, RequestInterceptor}
import utopia.nexus.controller.api.node.ApiNode
import utopia.nexus.controller.write.ContentWriter
import utopia.nexus.model.api.ApiVersion
import utopia.nexus.model.api.PathFollowResult.{Follow, NotFound, Ready, Redirected}
import utopia.nexus.model.request.{Request, StreamOrReader}
import utopia.nexus.model.response.{RequestResult, Response, ResponseContent}

import scala.annotation.tailrec
import scala.collection.{View, mutable}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object ApiRoot
{
	// COMPUTED    -------------------------
	
	/**
	 * Initializes a new API builder factory, using the default request body type (a stream)
	 * @return A builder factory for setting up an [[ApiRoot]] instance
	 */
	def default = expectingBody[StreamOrReader]
	/**
	 * Initializes a new API builder factory that supports a specific request body type
	 * @tparam Body Type of the expected request body
	 * @return A builder factory for setting up an [[ApiRoot]] instance
	 */
	def expectingBody[Body] = new ApiBuilderFactory[Body]
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectAsBuilderFactory(o: ApiRoot.type): ApiBuilderFactory[StreamOrReader] = o.default
	
	
	// NESTED   ---------------------------
	
	/**
	 * Specifies expected request body type. Provides a function for creating a new ApiBuilder.
	 * @tparam Body Type of the expected request body
	 */
	class ApiBuilderFactory[Body]
	{
		/**
		 * Creates a new API builder
		 * @param rootPath Path to the API root node (i.e. the last node before the version number).
		 *                 Typically, this doesn't include elements from the server domain.
		 *
		 *                 E.g. "api" or "my-service/api".
		 * @param contentWriter Interface used for converting [[ResponseContent]] from [[RequestResult]]
		 *                      into response body content.
		 *
		 *                      See [[ContentWriter]] companion object for existing implementations.
		 * @param latestVersion The latest API version. Default = v1.
		 *                      Note: Will always be adjusted to include all versions mentioned in 'nodesByVersion'.
		 * @param prepareContext A function for preparing the request context.
		 *                       Receives:
		 *                          1. The received request
		 *                          1. The targeted API version
		 * @param log An implicit logging implementation which receives errors thrown during request-processing and
		 *            interceptor-closing.
		 * @tparam C Type of the request context used
		 * @return A builder for setting up an [[ApiRoot]] instance
		 */
		def newBuilder[C <: AutoCloseable](rootPath: String, contentWriter: ContentWriter[C],
		                                   latestVersion: ApiVersion = ApiVersion.v1)
		                                  (prepareContext: (Request[Body], ApiVersion) => C)
		                                  (implicit log: Logger) =
			new ApiBuilder[C, Body](Regex.forwardSlash.split(rootPath), contentWriter, latestVersion, prepareContext)
	}
	
	class ApiBuilder[C <: AutoCloseable, B](rootPath: Seq[String], contentWriter: ContentWriter[C],
	                                        latestVersion: ApiVersion,
	                                        prepareContext: (Request[B], ApiVersion) => C)
	                                       (implicit log: Logger)
		extends mutable.Builder[ApiNode[C], ApiRoot[C, B]]
	{
		// ATTRIBUTES   ------------------
		
		private val commonNodesBuilder = OptimizedIndexedSeq.newBuilder[ApiNode[C]]
		private val versionedNodeBuilders = mutable.Map[ApiVersion, OptimizedSeqBuilder[ApiNode[C]]]()
		private val interceptorsBuilder = OptimizedIndexedSeq.newBuilder[InterceptRequest[C]]
		
		
		// IMPLEMENTED  -----------------
		
		override def clear(): Unit = {
			commonNodesBuilder.clear()
			versionedNodeBuilders.clear()
			interceptorsBuilder.clear()
		}
		override def result(): ApiRoot[C, B] = {
			val commonNodes = commonNodesBuilder.result()
			val nodes = {
				// Case: No versioning applied => Places all nodes under v1
				if (versionedNodeBuilders.isEmpty)
					Map(ApiVersion.v1 -> commonNodes)
				// Case: Versions don't share nodes
				else if (commonNodes.isEmpty)
					versionedNodeBuilders.view.mapValues { _.result() }.toMap
				// Case: Versions share nodes
				else {
					val default = versionedNodeBuilders.view
						.mapValues { builder =>
							val customNodes = builder.result()
							// Case: This version has no custom nodes => Applies the common nodes
							if (customNodes.isEmpty)
								commonNodes
							// Case: This version has custom nodes => Joins the nodes using a view only
							else
								View.concat(commonNodes, customNodes)
						}
						.toMap
					// Makes sure v1 is included
					if (default.contains(ApiVersion.v1))
						default
					else
						default + (ApiVersion.v1 -> commonNodes)
				}
			}
			new ApiRoot[C, B](nodes, contentWriter, rootPath, latestVersion, interceptorsBuilder.result())(
				prepareContext)
		}
		
		override def addOne(elem: ApiNode[C]): ApiBuilder.this.type = {
			commonNodesBuilder += elem
			this
		}
		override def addAll(elems: IterableOnce[ApiNode[C]]): ApiBuilder.this.type = {
			commonNodesBuilder ++= elems
			this
		}
		
		
		// OTHER    --------------------
		
		/**
		 * Adds a new version-specific node to this API
		 * @param node The applicable version, and the node to add
		 * @return This builder
		 */
		def +=(node: (ApiVersion, ApiNode[C])) = {
			apply(node._1) += node._2
			this
		}
		/**
		 * Adds 0-n new version-specific nodes to this API
		 * @param nodes The applicable version, and the nodes to add
		 * @return This builder
		 */
		def ++=(nodes: (ApiVersion, IterableOnce[ApiNode[C]])) = {
			apply(nodes._1) ++= nodes._2
			this
		}
		
		/**
		 * Adds a new request interceptor
		 * @param interceptor The interception logic to include
		 * @return This builder
		 */
		def +=(interceptor: InterceptRequest[C]) = {
			interceptorsBuilder += interceptor
			this
		}
		
		/**
		 * @param version Targeted API version
		 * @return A builder for adding (custom) nodes to that version
		 *         (nodes common to all versions should be added directly to this builder instead)
		 */
		def apply(version: Int): mutable.Growable[ApiNode[C]] = apply(ApiVersion(version))
		/**
		 * @param version Targeted API version
		 * @return A builder for adding (custom) nodes to that version
		 *         (nodes common to all versions should be added directly to this builder instead)
		 */
		def apply(version: ApiVersion): mutable.Growable[ApiNode[C]] =
			versionedNodeBuilders.getOrElseUpdate(version, OptimizedIndexedSeq.newBuilder[ApiNode[C]])
	}
}

/**
 * Represents the topmost API node.
 * Used for processing API requests by identifying and utilizing the targeted ApiNode.
 * Supports versioning.
 *
 * @param nodesByVersion A map where keys are API versions and values are the root nodes present in that version.
 *                       In cases where versions are missing, a previous version's nodes are used instead.
 * @param contentWriter Interface used for converting [[ResponseContent]] from [[RequestResult]]
 *                      into response body content.
 *
 *                      See [[ContentWriter]] companion object for existing implementations.
 *
 * @param latestVersion The latest API version. Default = v1.
 *                      Note: Will always be adjusted to include all versions mentioned in 'nodesByVersion'.
 * @param interceptors Interceptors used for recording and/or modifying the received requests,
 *                     request context and outgoing request results.
 *                     Default = empty.
 * @param prepareContext A function for preparing the request context.
 *                       Receives:
 *                          1. The received request
 *                          1. The targeted API version
 * @param log An implicit logging implementation which receives errors thrown during request-processing and
 *               interceptor-closing.
 *
 * @tparam C    Type of request context used (typically [[utopia.nexus.model.request.RequestContext]] or a lower type)
 * @tparam Body Type of request bodies accepted (typically [[utopia.nexus.model.request.StreamOrReader]]).
 *
 * @author Mikko Hilpinen
 * @since 06.11.2025, based on RequestHandler written 9.9.2017
 */
class ApiRoot[C <: AutoCloseable, -Body](nodesByVersion: Map[ApiVersion, Iterable[ApiNode[C]]],
                                         contentWriter: ContentWriter[C], rootPath: Seq[String] = Empty,
                                         latestVersion: ApiVersion = ApiVersion.v1,
                                         interceptors: Iterable[InterceptRequest[C]])
                                        (prepareContext: (Request[Body], ApiVersion) => C)
                                        (implicit log: Logger)
{
	// ATTRIBUTES   -----------------------
	
	private val _latestVersion = nodesByVersion.keysIterator.max max latestVersion
	
	
	// OTHER    ---------------------------
	
	/**
	 * Serves the specified request by finding and utilizing the ApiNode targeted by that request
	 * @param request Request to fulfill, if possible
	 * @return A response generated for the specified request
	 */
	def apply(request: Request[Body]): Response = {
		val (appliedRequest, interceptors) = intercept(request)
		val result = findTargetedVersion(appliedRequest.path, 0, rootPath.iterator) match {
			case Right((version, nodes, path)) =>
				// Acquires the request context
				withContext(appliedRequest, version, path, interceptors) { implicit context =>
					// Catches exceptions and wraps them as internal server errors
					Try[RequestResult] { _apply(version, nodes, path, appliedRequest.method, interceptors) }
						.getOrMap[RequestResult] { error =>
							// In addition to sending error data forward, logs it
							log(error, s"Failed to handle $appliedRequest in API $version")
							InternalServerError -> "Unexpected failure while processing the request"
						}
				}
			// Case: The request didn't target this node, or didn't target an existing version => Yields 404
			case Left(failureMessage) =>
				withContext(appliedRequest, _latestVersion, Empty, interceptors) { implicit context =>
					intercept((Status.NotFound, failureMessage): RequestResult, interceptors) {
						_.interceptNotFound(_, Empty)
					}
				}
		}
		interceptors.foreach { _.closeQuietly().logWithMessage("Failed to close a request interceptor") }
		result
	}
	
	private def _apply(version: ApiVersion, rootNodes: Iterable[ApiNode[C]], path: Seq[String], method: Method,
	                   interceptors: Iterable[RequestInterceptor[C]])
	                  (implicit context: C) =
	{
		// Finds the targeted API node
		val nodeFindResult = {
			if (path.isEmpty)
				Right(new RootNode(version, rootNodes) -> Empty)
			else
				findTargetedNode(rootNodes, path, version)
		}
		nodeFindResult match {
			// Case: Node found => Executes the method, if allowed
			case Right((node, remainingPath)) =>
				tryExecute(method, node, remainingPath, interceptors)
			// Case: No targeted node found => Yields the generated failure after interception
			case Left(notFoundResult) =>
				intercept(notFoundResult, interceptors) { _.interceptNotFound(_, path) }
		}
	}
	
	private def tryExecute(method: Method, node: ApiNode[C], remainingPath: Seq[String],
	                       interceptors: Iterable[RequestInterceptor[C]])
	                      (implicit context: C) =
	{
		val allowed = node.allowedMethods
		// Case: Method supported => Fulfills the request using the targeted API node
		if (allowed.exists { _ == method }) {
			interceptors.foreach { _.beforeExecution(method) }
			val result = node(method, remainingPath)
			intercept(result, interceptors) { _.interceptNodeResult(method, _) }
		}
		// Case: Method not supported => Yields 405 or 501
		else {
			val failure: RequestResult = {
				// Case: No methods are supported => 501
				if (allowed.isEmpty)
					NotImplemented -> s"Node \"${ node.name }\" is not implemented at this time"
				// Case: Method not supported => 405
				else
					RequestResult(ResponseContent(
						Model.from("allowedMethods" -> allowed.view.map { _.name }.toOptimizedSeq),
						s"$method is not allowed on node \"${ node.name }\""),
						MethodNotAllowed,
						Headers.empty.withAllowedMethods(allowed.toOptimizedSeq))
			}
			intercept(failure, interceptors) {
				_.interceptExecutionNotAllowed(_, method, allowed)
			}
		}
	}
	
	private def withContext(request: Request[Body], version: ApiVersion, path: Seq[String],
	                        interceptors: Iterable[RequestInterceptor[C]])
	                       (f: C => RequestResult) =
	{
		// Acquires the request context
		val (openContexts, appliedContext) = {
			val defaultContext = prepareContext(request, version)
			// Case: No interceptors => No context modifications
			if (interceptors.isEmpty)
				Single(defaultContext) -> defaultContext
			// Case: Interceptors present => Modifies the context incrementally and remembers each version
			else {
				val contexts = interceptors
					.foldLeftIterator(defaultContext) { (context, interceptor) =>
						interceptor.interceptContext(context, version, path)
					}
					.toOptimizedSeq
				contexts -> contexts.last
			}
		}
		// Performs the execution using the acquired context, and converts the result into a response
		try { resultToResponse(f(appliedContext))(appliedContext) }
		finally {
			// Finally closes all open context instances
			openContexts.reverseIterator
				.foreach { _.closeQuietly().logWithMessage("Failed to close a request context") }
		}
	}
	
	/**
	 * Converts a RequestResult into a Response
	 * @param result The result to convert
	 * @param context Required contextual information
	 * @return A response from the specified result
	 */
	private def resultToResponse(result: RequestResult)(implicit context: C) = {
		val (body, appliedStatus) = result.output match {
			case Right(content) => contentWriter.prepare(content, result.status, result.headers)
			case Left(body) => body -> result.status
		}
		val appliedHeaders = result.headers.withContentType(body.contentType).withContentLength(body.contentLength)
		Response(appliedStatus, appliedHeaders, body = body)
	}
	
	private def intercept[B](request: Request[B]): (Request[B], Seq[RequestInterceptor[C]]) = {
		if (interceptors.isEmpty)
			request -> Empty
		else {
			val interceptorsBuilder = OptimizedIndexedSeq.newBuilder[RequestInterceptor[C]]
			val modifiedRequest = intercept(request, interceptors) { (interceptor, request) =>
				val (modifiedRequest, newInterceptor) = interceptor.intercept(request)
				interceptorsBuilder ++= newInterceptor
				modifiedRequest
			}
			modifiedRequest -> interceptorsBuilder.result()
		}
	}
	private def intercept[A, I](unmodified: A, interceptors: Iterable[I])(intercept: (I, A) => A) =
		interceptors.foldLeft(unmodified) { (item, interceptor) => intercept(interceptor, item) }
	
	@tailrec
	private def findTargetedVersion(path: Seq[String], nextIndex: Int,
	                                skipIterator: Iterator[String]): Either[String, (ApiVersion, Iterable[ApiNode[C]], Seq[String])] =
	{
		path.lift(nextIndex) match {
			case Some(nextStep) =>
				skipIterator.nextOption() match {
					// Case: Path remains to be skipped => Makes sure the specified path matches the skipping path
					case Some(stepToSkip) =>
						if (nextStep ~== stepToSkip)
							findTargetedVersion(path, nextIndex + 1, skipIterator)
						else
							Left(s"Expected \"$stepToSkip\", found \"$nextStep\"")
					// Case: No more path to skip => Expects the next path step to contain the targeted version
					case None =>
						ApiVersion.parse(nextStep) match {
							case Success(version) =>
								version.decreasing.findMap(nodesByVersion.get) match {
									// Case: Found a valid version => Continues with the resources in that version
									case Some(nodes) => Right(version, nodes, path.drop(nextIndex + 1))
									// Case: Targeted version not found => Fails
									case None =>
										Left(s"\"$nextStep\" is not a supported version. Available versions are: [${
											nodesByVersion.keys.toOptimizedSeq.sorted.mkString(", ")
										}]")
								}
							case Failure(_) =>
								Left(s"\"$nextStep\" is not a valid version number. Available versions are: [${
									nodesByVersion.keys.toOptimizedSeq.sorted.mkString(", ")
								}]")
						}
				}
			// Case: The path was too short => Fails
			case None =>
				Left(skipIterator.nextOption() match {
					case Some(expectedStep) => s"Expected the request path to continue with /$expectedStep"
					// Case: Version number was mising
					case None =>
						s"Expected request path to continue with a version. Available versions: [${
							nodesByVersion.keys.mkString(", ")
						}]"
				})
		}
	}
	
	@tailrec
	private def findTargetedNode[C2 <: C](rootNodes: Iterable[ApiNode[C2]], path: Seq[String], version: ApiVersion)
	                                     (implicit context: C2): Either[RequestResult, (ApiNode[C2], Seq[String])] =
	{
		val firstStep = path.head
		rootNodes.find { _.name ~== firstStep } match {
			// Case: Root node found => follows it
			case Some(root) =>
				follow(root, path, 1) match {
					case Right(node) => Right(node)
					case Left(Left(failure)) => Left(failure)
					case Left(Right(newPath)) => findTargetedNode[C2](rootNodes, newPath, version)
				}
			// Case: Root node not found => Fails
			case None =>
				Left(Status.NotFound ->
					s"\"$firstStep\" couldn't be found under ${
						rootPath.mkString("/") }/$version; The available nodes are: [${
						rootNodes.iterator.map { _.name }.mkString(", ") }]")
			}
	}
	
	/**
	 * Follows the request path, finding the targeted API node. Recursive.
	 * @param lastNode The last identified API node
	 * @param path The targeted request path, excluding the root path
	 * @param nextIndex Next targeted index on 'path'
	 * @param context Implicit request context
	 * @tparam C2 Type of the used context
	 * @return Either:
	 *              - Right: The targeted API node, and the unexplored part of the request path
	 *              - Left: Either a failure result (left), or a new path to explore (right)
	 */
	@tailrec
	private def follow[C2 <: C](lastNode: ApiNode[C2], path: Seq[String], nextIndex: Int)
	                           (implicit context: C2): Either[Either[RequestResult, Seq[String]], (ApiNode[C2], Seq[String])] =
		path.lift(nextIndex) match {
			case Some(nextStep) =>
				lastNode.follow(nextStep) match {
					case Ready => Right(lastNode -> path.drop(nextIndex))
					case Follow(next) => follow(next, path, nextIndex + 1)
					case failure: NotFound => Left(Left(failure.toRequestResult))
					case Redirected(newPath, noRemainder) =>
						val fullPath = if (noRemainder) newPath else newPath ++ path.view.drop(nextIndex + 1)
						Left(Right(fullPath))
				}
			case None => Right(lastNode -> Empty)
		}
		
	
	// NESTED   ------------------------
	
	/**
	 * A node used for handling a request targeting this ApiRoot
	 * @param version The targeted API version
	 * @param nodes API nodes available within this version
	 */
	private class RootNode(version: ApiVersion, nodes: Iterable[ApiNode[_]]) extends ApiNode[Any]
	{
		override lazy val name: String = rootPath.lastOption.getOrElse("api")
		override val allowedMethods: Iterable[Method] = Single(Get)
		
		override def follow(step: String)(implicit context: Any) = NotFound("follow is not supported in the root node")
		
		override def apply(method: Method, remainingPath: Seq[String])(implicit context: Any): RequestResult =
			RequestResult(Model(Vector(
				"version" -> version.toString, "nodes" -> nodes.view.map { _.name }.toOptimizedSeq,
				"otherVersions" -> nodesByVersion.keysIterator.filterNot { _ == version }.toOptimizedSeq
					.sorted.map { _.toString })))
	}
}