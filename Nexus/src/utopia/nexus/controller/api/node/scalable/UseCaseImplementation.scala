package utopia.nexus.controller.api.node.scalable

import utopia.flow.view.immutable.caching.Lazy
import utopia.nexus.http.Path
import utopia.nexus.model.response.RequestResult

object UseCaseImplementation
{
	// COMPUTED --------------------------------
	
	/**
	 * @return Access to constructors that don't accept the default request result
	 */
	def default = DefaultImplementation
	
	
	// OTHER    --------------------------------
	
	/**
	 * Creates a use-case implementation by wrapping a function
	 * @param f A function to wrap.
	 *          Accepts:
	 *              1. Applicable request context
	 *              1. Remaining request path after the targeted node
	 *              1. A lazily available default result
	 *
	 *          Yields the result to send to the client
	 * @tparam C Type of the used request context
	 * @return A use-case implementation wrapping the specified function
	 * @see [[usingNodeContext]]
	 */
	def apply[C](f: (C, Seq[String], Lazy[RequestResult]) => RequestResult) =
		usingNodeContext[Any, C] { (_, context, path, default) => f(context, path, default) }
	/**
	 * Creates a use-case implementation that utilizes node-specific context
	 * @param f A function to wrap.
	 *          Accepts:
	 *              1. Node-specific context
	 *              1. Applicable request context
	 *              1. Remaining request path after the targeted node
	 *              1. A lazily available default result
	 *
	 *          Yields the result to send to the client
	 * @tparam P Type of the used node-specific context
	 * @tparam C Type of the used request context
	 * @return A use-case implementation wrapping the specified function
	 */
	def usingNodeContext[P, C](f: (P, C, Seq[String], Lazy[RequestResult]) => RequestResult): UseCaseImplementation[C, P] =
		new _UseCaseImplementation[C, P](f)
	
	/**
	 * Creates a new use case implementation that utilizes resource-specific context parameter
	 * @param f Function that accepts 1) Request context, 2) Resource-specific context parameter,
	 *          3) remaining request path and produces a response
	 * @tparam C Type of expected request context
	 * @tparam P Type of expected resource-specific parameter
	 * @return A new use case implementation
	 */
	@deprecated("Deprecated for removal. Replaced with default.usingNodeContext(...); Notice the different parameter-ordering.", "v2.0")
	def defaultUsingContext[C, P](f: (C, P, Option[Path]) => RequestResult) =
		usingContext[C, P] { (context, param, path, _) => f(context, param, path) }
	/**
	 * Creates a new use case implementation that utilizes resource-specific context parameter
	 * @param f Function that accepts 1) Request context, 2) Resource-specific context parameter,
	 *          3) remaining request path and 4) default response (call by name) and produces a response
	 * @tparam C Type of expected request context
	 * @tparam P Type of expected resource-specific parameter
	 * @return A new use case implementation
	 */
	@deprecated("Deprecated for removal. Replaced with .usingNodeContext(...); Notice the different parameter-ordering.", "v2.0")
	def usingContext[C, P](f: (C, P, Option[Path], Lazy[RequestResult]) => RequestResult): UseCaseImplementation[C, P] =
		usingNodeContext[P, C] { (nodeContext, context, path, default) =>
			f(context, nodeContext, if (path.isEmpty) Some(Path(path)) else None, default)
		}
	
	
	// NESTED   --------------------------------
	
	object DefaultImplementation
	{
		/**
		 * Creates a use-case implementation by wrapping a function
		 * @param f A function to wrap.
		 *          Accepts:
		 *              1. Applicable request context
		 *              1. Remaining request path after the targeted node
		 *
		 *          Yields the result to send to the client
		 * @tparam C Type of the used request context
		 * @return A use-case implementation wrapping the specified function
		 * @see [[usingNodeContext]]
		 */
		def apply[C](f: (C, Seq[String]) => RequestResult) =
			usingNodeContext[Any, C] { (_, context, path) => f(context, path) }
		/**
		 * Creates a use-case implementation that utilizes node-specific context
		 * @param f A function to wrap.
		 *          Accepts:
		 *              1. Node-specific context
		 *              1. Applicable request context
		 *              1. Remaining request path after the targeted node
		 *
		 *          Yields the result to send to the client
		 * @tparam P Type of the used node-specific context
		 * @tparam C Type of the used request context
		 * @return A use-case implementation wrapping the specified function
		 */
		def usingNodeContext[P, C](f: (P, C, Seq[String]) => RequestResult): UseCaseImplementation[C, P] =
			UseCaseImplementation
				.usingNodeContext[P, C] { (nodeContext, context, path, _) => f(nodeContext, context, path) }
	}
	
	private class _UseCaseImplementation[-C, -P](f: (P, C, Seq[String], Lazy[RequestResult]) => RequestResult)
		extends UseCaseImplementation[C, P]
	{
		override def apply(remainingPath: Seq[String], nodeContext: P, default: Lazy[RequestResult])
		                  (implicit context: C): RequestResult =
			f(nodeContext, context, remainingPath, default)
	}
}

/**
 * A common trait for method-specific API node use-case implementations (e.g. for a GET implementation)
 * @tparam C Type of the required request context
 * @tparam P Type of the required node-specific context (Any if no node-specific context is required)
 * @author Mikko Hilpinen
 * @since 17.6.2021, v1.6
 */
trait UseCaseImplementation[-C, -P]
{
	// ABSTRACT --------------------------------
	
	/**
	 * Handles a request using this implementation
	 * @param remainingPath Remaining request path after the targeted API node.
	 *                      Empty if the request path ends at the node executing the request.
	 * @param nodeContext Node-specific context information
	 * @param default Result generated by the default / next available implementation (lazy)
	 * @param context Request context (implicit)
	 * @return Request result to yield to the client
	 */
	def apply(remainingPath: Seq[String], nodeContext: P, default: Lazy[RequestResult])
	         (implicit context: C): RequestResult
	
	
	// OTHER    ----------------------------------
	
	/**
	 * Handles a request using this implementation
	 * @param remainingPath Remaining request path after the targeted API node.
	 *                      Empty if the request path ends at the node executing the request.
	 * @param nodeContext Node-specific context information
	 * @param default Result generated by the default / next available implementation (called lazily)
	 * @param context Request context (implicit)
	 * @return Request result to yield to the client
	 */
	def apply(remainingPath: Seq[String], nodeContext: P)(default: => RequestResult)
	         (implicit context: C): RequestResult =
		apply(remainingPath, nodeContext, Lazy(default))
}
