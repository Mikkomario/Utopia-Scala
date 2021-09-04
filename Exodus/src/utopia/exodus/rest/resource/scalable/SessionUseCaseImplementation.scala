package utopia.exodus.rest.resource.scalable

import utopia.access.http.Method
import utopia.exodus.model.stored.UserSession
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.rest.util.AuthorizedContext.SessionParams
import utopia.flow.datastructure.immutable.Lazy
import utopia.nexus.http.Path
import utopia.nexus.rest.scalable.UseCaseImplementation
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * An object that offers methods for constructing session authorized use-case implementations
 * @author Mikko Hilpinen
 * @since 18.6.2021, v1.1
 */
object SessionUseCaseImplementation
{
	/**
	 * Creates a new session authorized use case implementation
	 * @param method Method expected by this implementation
	 * @param f A function that accepts 1) active user session, 2) database connection, 3) request context,
	 *          4) remaining request path and 5) Lazy default result and produces a result
	 * @return A new use case implementation
	 */
	def apply(method: Method)(f: (UserSession, Connection, AuthorizedContext, Option[Path], Lazy[Result]) => Result) =
		UseCaseImplementation.usingContext[AuthorizedContext, SessionParams](method) { (context, params, path, default) =>
			f(params._1, params._2, context, path, default)
		}
	
	/**
	 * Creates a new session authorized use case implementation
	 * @param method Method expected by this implementation
	 * @param f A function that accepts 1) active user session, 2) database connection, 3) request context,
	 *          4) remaining request path and produces a result
	 * @return A new use case implementation
	 */
	def default(method: Method)(f: (UserSession, Connection, AuthorizedContext, Option[Path]) => Result) =
		apply(method) { (session, connection, context, path, _) => f(session, connection, context, path) }
	
	/**
	  * Creates a new session authorized use case implementation factory
	  * @param method Method expected by this implementation
	  * @param f A function that accepts 1) external parameter 2) active user session,
	  *          3) database connection, 4) request context, 5) remaining request path and
	  *          6) Lazy default result and produces a result
	  * @return A function for generating new use case implementations based on external parameters
	  */
	def factory[A](method: Method)
	           (f: (A, UserSession, Connection, AuthorizedContext, Option[Path], Lazy[Result]) => Result) =
	{
		parameter: A => UseCaseImplementation.usingContext[AuthorizedContext, SessionParams](method) {
			(context, params, path, default) =>
				f(parameter, params._1, params._2, context, path, default)
		}
	}
}
