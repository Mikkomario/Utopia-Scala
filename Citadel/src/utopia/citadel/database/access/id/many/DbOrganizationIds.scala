package utopia.citadel.database.access.id.many

import utopia.citadel.database.model.organization.OrganizationModel
import utopia.vault.nosql.access.many.column.ManyIntIdAccess

/**
  * Used for accessing multiple organization ids at a time
  * @author Mikko Hilpinen
  * @since 13.5.2020, v1.0
  */
object DbOrganizationIds extends ManyIntIdAccess
{
	// IMPLEMENTED	---------------------
	
	override def target = factory.table
	
	override def table = factory.table
	
	override def globalCondition = None
	
	
	// COMPUTED	------------------------
	
	private def factory = OrganizationModel
}
