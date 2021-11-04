package utopia.exodus.rest.resource.scalable

import utopia.access.http.Method
import utopia.exodus.model.stored.auth.SessionToken
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.rest.util.AuthorizedContext.OrganizationParams
import utopia.flow.datastructure.immutable.Lazy
import utopia.nexus.http.Path
import utopia.nexus.rest.scalable.UseCaseImplementation
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * An object that offers methods for constructing session authorized organization-specific use-case
 * implementations (in factory format)
 * @author Mikko Hilpinen
 * @since 18.6.2021, v1.1
 */
object OrganizationUseCaseImplementation
{
	/**
	 * Creates a new session authorized use case implementation factory
	 * @param method Method expected by this implementation
	 * @param f A function that accepts 1) active user session, 2) User membership id
	 *          3) database connection, 4) request context, 5) remaining request path and
	 *          6) Lazy default result and produces a result
	 * @return A function for generating new use case implementations based on organization id
	 */
	def apply(method: Method)
	           (f: (SessionToken, Int, Connection, AuthorizedContext, Option[Path], Lazy[Result]) => Result) =
		UseCaseImplementation.usingContext[AuthorizedContext, OrganizationParams](method) {
			(context, params, path, default) => f(params._1, params._2, params._3, context, path, default)
		}
	
	/**
	 * Creates a new session authorized use case implementation factory
	 * @param method Method expected by this implementation
	 * @param f A function that accepts 1) active user session, 2) User membership id
	 *          3) database connection, 4) request context, 5) remaining request path
	 * @return A function for generating new use case implementations based on organization id
	 */
	def default(method: Method)(f: (SessionToken, Int, Connection, AuthorizedContext, Option[Path]) => Result) =
		apply(method) { (session, membershipId, connection, context, path, _) =>
			f(session, membershipId, connection, context, path)
		}
	
	/**
	 * Creates a new session authorized use case implementation factory for organization-specific nodes
	 * @param method Method expected by this implementation
	 * @param f A function that accepts 1) Target organization id 2) active user session, 3) User membership id
	 *          4) database connection, 5) request context, 6) remaining request path and
	 *          7) Lazy default result and produces a result
	 * @return A function for generating new use case implementations based on organization id
	 */
	def factory(method: Method)
	         (f: (Int, SessionToken, Int, Connection, AuthorizedContext, Option[Path], Lazy[Result]) => Result) =
	{
		organizationId: Int => UseCaseImplementation.usingContext[AuthorizedContext, OrganizationParams](method) {
			(context, params, path, default) =>
				f(organizationId, params._1, params._2, params._3, context, path, default)
		}
	}
}
