package utopia.nexus.rest.scalable

import utopia.access.http.Method
import utopia.access.http.Status.NotImplemented
import utopia.flow.util.CollectionExtensions._
import utopia.nexus.http.{Path, Response}
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.rest.{Context, Resource}
import utopia.nexus.result.Result

/**
 * A resource which supports extensions that provide custom functionality over the default implementation.
 * Use this trait if you can't / don't want to specify all resource functionality within a single module
 * (E.g. when creating an extendable framework)
 * @author Mikko Hilpinen
 * @since 17.6.2021, v1.6
 */
trait ModularResource[-C <: Context, P] extends Resource[C]
{
	// ABSTRACT ---------------------------
	
	/**
	 * Wraps the custom implementation into a response, performing the necessary pre- and post processing
	 * @param implementation Function that handles the request
	 * @param context Implicit request context
	 * @return Response based on the context and/or implementation
	 */
	protected def wrap(implementation: P => Result)(implicit context: C): Response
	
	/**
	 * @return The use case implementations used in this resource, grouped by method
	 */
	def useCaseImplementations: Map[Method, Iterable[UseCaseImplementation[C, P]]]
	/**
	 * @return The follow implementations used in this resource
	 */
	def followImplementations: Iterable[FollowImplementation[C]]
	
	
	// IMPLEMENTED  -----------------------
	
	override def allowedMethods = useCaseImplementations.keySet
	
	override def toResponse(remainingPath: Option[Path])(implicit context: C) = {
		// Handles the initial authorization & processing
		wrap { param =>
			// Finds the applicable use cases
			val method = context.request.method
			val useCaseIterator = useCaseImplementations.get(method).iterator.flatten
			def nextResult(): Result = useCaseIterator.nextOption() match {
				case Some(implementation) => implementation(remainingPath, param) { nextResult() }
				case None => Result.Failure(NotImplemented)
			}
			nextResult()
		}
	}
	
	override def follow(path: Path)(implicit context: C) =
		followImplementations.findMap { _(path) }.getOrElse { Error() }
}
