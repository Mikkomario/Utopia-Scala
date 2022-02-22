package utopia.exodus.rest.resource.scalable

import utopia.exodus.model.stored.auth.Token
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.rest.util.AuthorizedContext.OrganizationParams
import utopia.nexus.rest.scalable.ExtendableResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * An abstract class for resources that use organization-specific session authorization
 * and provide extension capability
 * @author Mikko Hilpinen
 * @since 25.6.2021, v1.1
 * @param organizationId Id of the organization targeted in this resource
 */
abstract class ExtendableOrganizationResource(val organizationId: Int)
	extends ExtendableResource[AuthorizedContext, OrganizationParams]
{
	override protected def wrap(implementation: ((Token, Int, Connection)) => Result)
	                           (implicit context: AuthorizedContext) =
		context.authorizedInOrganization(organizationId) { (session, membershipId, connection) =>
			implementation(session, membershipId, connection)
		}
}
