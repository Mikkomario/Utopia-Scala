package utopia.exodus.rest.resource.scalable

import utopia.access.http.Method
import utopia.exodus.model.stored.auth.Token
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.rest.util.AuthorizedContext.OrganizationParams
import utopia.flow.view.immutable.caching.Lazy
import utopia.nexus.http.Path
import utopia.nexus.rest.scalable.{ExtendableResource, ExtendableResourceFactory}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * A common class for organization-specific resource factories that support extensions
 * @author Mikko Hilpinen
 * @since 25.6.2021, v2.0
 */
abstract class ExtendableOrganizationResourceFactory[+R <: ExtendableResource[AuthorizedContext, OrganizationParams]]
	extends ExtendableResourceFactory[Int, AuthorizedContext, OrganizationParams, R]
{
	/**
	 * Adds a new use case to all resources that will be created through this factory
	 * @param method Method expected in the use case
	 * @param useCase A function that accepts 1) target organization id, 2) user session, 3) user membership id,
	 *                4) database connection, 5) request context, 6) remaining request path and
	 *                7) default implementation (lazy) and returns a result
	 */
	def addUseCase(method: Method)(useCase: (Int, Token, Int, Connection, AuthorizedContext, Option[Path],
		Lazy[Result]) => Result): Unit =
		addUseCase(method, OrganizationUseCaseImplementation
			.factory { (organizationId, session, membershipId, connection, context, path, default) =>
				useCase(organizationId, session, membershipId, connection, context, path, default)
			})
}
