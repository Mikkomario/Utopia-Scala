package utopia.citadel.database.access.id.many

import utopia.citadel.database.model.organization.OrganizationModel
import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.access.ManyIdAccess

/**
  * Used for accessing multiple organization ids at a time
  * @author Mikko Hilpinen
  * @since 13.5.2020, v1
  */
object DbOrganizationIds extends ManyIdAccess[Int]
{
	// IMPLEMENTED	---------------------
	
	override def target = factory.table
	
	override def valueToId(value: Value) = value.int
	
	override def table = factory.table
	
	override def globalCondition = None
	
	
	// COMPUTED	------------------------
	
	private def factory = OrganizationModel
}
