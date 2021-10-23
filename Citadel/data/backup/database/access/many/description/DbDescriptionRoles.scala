package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionRoleFactory
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.nosql.access.many.model.ManyRowModelAccess

/**
  * Used for accessing multiple description roles at a time
  * @author Mikko Hilpinen
  * @since 27.6.2021, v1.0
  */
object DbDescriptionRoles
	extends ManyRowModelAccess[DescriptionRole] with ManyDescribedAccess[DescriptionRole, DescribedDescriptionRole]
{
	override def factory = DescriptionRoleFactory
	
	override protected def defaultOrdering = None
	
	override def globalCondition = None
	
	override protected def manyDescriptionsAccess = DbDescriptionRoleDescriptions
	
	override protected def describedFactory = DescribedDescriptionRole
	
	override protected def idOf(item: DescriptionRole) = item.id
}
