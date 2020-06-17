package utopia.exodus.database.access.id

import utopia.exodus.database.model.organization.OrganizationModel
import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.access.ManyIdAccess

/**
  * Used for accessing multiple organization ids at a time
  * @author Mikko Hilpinen
  * @since 13.5.2020, v2
  */
object OrganizationIds extends ManyIdAccess[Int]
{
	// IMPLEMENTED	---------------------
	
	override def target = factory.table
	
	override def valueToId(value: Value) = value.int
	
	override def table = factory.table
	
	override def globalCondition = None
	
	
	// COMPUTED	------------------------
	
	private def factory = OrganizationModel
}
