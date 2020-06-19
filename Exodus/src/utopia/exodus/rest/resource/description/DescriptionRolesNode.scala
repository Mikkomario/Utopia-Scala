package utopia.exodus.rest.resource.description

import utopia.exodus.database.access.many.DbDescriptions
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.enumeration.DescriptionRole
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection

/**
  * Used for retrieving descriptions of all description roles
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
object DescriptionRolesNode extends PublicDescriptionsNode[DescriptionRole, DescribedDescriptionRole]
{
	// IMPLEMENTED	------------------------
	
	override val name = "description-roles"
	
	override protected def items(implicit connection: Connection) = DescriptionRole.values
	
	override protected def descriptionsAccess = DbDescriptions.ofAllDescriptionRoles
	
	override protected def idOf(item: DescriptionRole) = item.id
	
	override protected def combine(item: DescriptionRole, descriptions: Set[DescriptionLink]) =
		DescribedDescriptionRole(item, descriptions)
}
