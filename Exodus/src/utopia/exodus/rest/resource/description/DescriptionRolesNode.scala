package utopia.exodus.rest.resource.description

import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.exodus.rest.util.AuthorizedContext
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.enumeration.ModelStyle.Simple
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object DescriptionRolesNode extends PublicDescriptionsNodeFactory[DescriptionRolesNode]
{
	override def apply(authorization: (AuthorizedContext, => Result, Connection) => Result) =
		new DescriptionRolesNode(authorization)
}

/**
  * Used for retrieving descriptions of all description roles
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  * @param authorization A function for authorizing incoming requests. Accepts
  */
class DescriptionRolesNode(authorization: (AuthorizedContext, => Result, Connection) => Result)
	extends PublicDescriptionsNode[DescriptionRole, DescribedDescriptionRole]
{
	// IMPLEMENTED	------------------------
	
	override val name = "description-roles"
	
	override def defaultModelStyle = Simple
	
	override protected def authorize(onAuthorized: => Result)
									(implicit context: AuthorizedContext, connection: Connection) =
		authorization(context, onAuthorized, connection)
	
	override protected def describedItems(implicit connection: Connection, languageIds: LanguageIds) =
		DbDescriptionRoles.described
}
