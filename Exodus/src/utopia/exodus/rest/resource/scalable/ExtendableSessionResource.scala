package utopia.exodus.rest.resource.scalable

import utopia.exodus.model.stored.auth.Token
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.rest.util.AuthorizedContext.SessionParams
import utopia.nexus.rest.scalable.ExtendableResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * An abstract class for resources that use session authorization and provide extension capability
 * @author Mikko Hilpinen
 * @since 18.6.2021, v1.1
 */
abstract class ExtendableSessionResource extends ExtendableResource[AuthorizedContext, SessionParams]
{
	override protected def wrap(implementation: ((Token, Connection)) => Result)
	                           (implicit context: AuthorizedContext) =
		context.authorizedWithoutScope { (session, connection) => implementation(session -> connection) }
}
