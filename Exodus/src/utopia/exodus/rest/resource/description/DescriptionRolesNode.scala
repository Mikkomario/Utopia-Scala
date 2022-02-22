package utopia.exodus.rest.resource.description

import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.exodus.rest.util.AuthorizedContext
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.nexus.rest.LeafResource
import utopia.vault.database.Connection

/**
  * Used for retrieving descriptions of all description roles
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
object DescriptionRolesNode extends GeneralDataNode[DescribedDescriptionRole] with LeafResource[AuthorizedContext]
{
	// IMPLEMENTED	------------------------
	
	override val name = "description-roles"
	
	override protected def describedItems(implicit connection: Connection, languageIds: LanguageIds) =
		DbDescriptionRoles.described
}
