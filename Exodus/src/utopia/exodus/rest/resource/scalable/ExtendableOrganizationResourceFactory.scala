package utopia.exodus.rest.resource.scalable

import utopia.access.http.Method
import utopia.exodus.model.stored.UserSession
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.rest.util.AuthorizedContext.OrganizationParams
import utopia.flow.datastructure.immutable.Lazy
import utopia.nexus.http.Path
import utopia.nexus.rest.scalable.{ExtendableResource, ExtendableResourceFactory}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * A common class for organization-specific resource factories that support extensions
 * @author Mikko Hilpinen
 * @since 25.6.2021, v1.1
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
	def addUseCase(method: Method)(useCase: (Int, UserSession, Int, Connection, AuthorizedContext, Option[Path],
		Lazy[Result]) => Result): Unit =
		addUseCase(OrganizationUseCaseImplementation
			.factory(method) { (organizationId, session, membershipId, connection, context, path, default) =>
				useCase(organizationId, session, membershipId, connection, context, path, default)
			})
}
