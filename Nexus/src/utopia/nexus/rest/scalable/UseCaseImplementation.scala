package utopia.nexus.rest.scalable

import utopia.flow.collection.immutable.caching.lazily.Lazy
import utopia.nexus.http.Path
import utopia.nexus.rest.Context
import utopia.nexus.result.Result

object UseCaseImplementation
{
	// OTHER    --------------------------------
	
	/**
	 * Creates a new use case implementation that doesn't utilize resource-specific context parameter
	 * @param f Function that accepts 1) Request context, 2) remaining request path and
	 *          3) default response (call by name) and produces a response
	 * @tparam C Type of expected request context
	 * @return A new use case implementation
	 */
	def apply[C <: Context](f: (C, Option[Path], Lazy[Result]) => Result): UseCaseImplementation[C, Any] =
		new FunctionalUseCaseImplementation[C, Any]((context, _, path, default) => f(context, path, default))
	
	/**
	  * Creates a new use case implementation that doesn't utilize resource-specific context parameter nor
	  * any other result function
	  * @param f Function that accepts 1) Request context, 2) remaining request path
	  * @tparam C Type of expected request context
	  * @return A new use case implementation
	  */
	def default[C <: Context](f: (C, Option[Path]) => Result): UseCaseImplementation[C, Any] =
		apply[C] { (context, path, _) => f(context, path) }
	
	/**
	 * Creates a new use case implementation that utilizes resource-specific context parameter
	 * @param f Function that accepts 1) Request context, 2) Resource-specific context parameter,
	 *          3) remaining request path and 4) default response (call by name) and produces a response
	 * @tparam C Type of expected request context
	 * @tparam P Type of expected resource-specific parameter
	 * @return A new use case implementation
	 */
	def usingContext[C <: Context, P](f: (C, P, Option[Path], Lazy[Result]) => Result): UseCaseImplementation[C, P] =
		new FunctionalUseCaseImplementation(f)
	
	/**
	 * Creates a new use case implementation that utilizes resource-specific context parameter
	 * @param f Function that accepts 1) Request context, 2) Resource-specific context parameter,
	 *          3) remaining request path and produces a response
	 * @tparam C Type of expected request context
	 * @tparam P Type of expected resource-specific parameter
	 * @return A new use case implementation
	 */
	def defaultUsingContext[C <: Context, P](f: (C, P, Option[Path]) => Result) =
		usingContext[C, P] { (context, param, path, _) => f(context, param, path) }
	
	
	// NESTED   --------------------------------
	
	private class FunctionalUseCaseImplementation[-C <: Context, -P](f: (C, P, Option[Path], Lazy[Result]) => Result)
		extends UseCaseImplementation[C, P]
	{
		override def apply(remainingPath: Option[Path], resourceContext: P, default: Lazy[Result])
		                  (implicit context: C) = f(context, resourceContext, remainingPath, default)
	}
}

/**
 * A common trait for rest node use case implementations (E.g. Get)
 * @author Mikko Hilpinen
 * @since 17.6.2021, v1.6
 */
trait UseCaseImplementation[-C <: Context, -P]
{
	// ABSTRACT --------------------------------
	
	/**
	 * Handles a request using this implementation
	 * @param remainingPath Remaining request path after this resource. None if the request path ends at this resource.
	 * @param resourceContext Resource specific context parameter
	 * @param default Response returned by the default / next available implementation (lazy)
	 * @param context Request context (implicit)
	 * @return Response to the request
	 */
	def apply(remainingPath: Option[Path], resourceContext: P, default: Lazy[Result])(implicit context: C): Result
	
	
	// OTHER    ----------------------------------
	
	/**
	 * Handles a request using this implementation
	 * @param remainingPath Remaining request path after this resource. None if the request path ends at this resource.
	 * @param resourceContext Resource specific context parameter
	 * @param default Response returned by the default / next available implementation (lazy)
	 * @param context Request context (implicit)
	 * @return Response to the request
	 */
	def apply(remainingPath: Option[Path], resourceContext: P)(default: => Result)(implicit context: C): Result =
		apply(remainingPath, resourceContext, Lazy { default })
}
