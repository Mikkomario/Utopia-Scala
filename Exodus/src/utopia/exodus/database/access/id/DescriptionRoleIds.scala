package utopia.exodus.database.access.id

import utopia.exodus.database.factory.description.DescriptionRoleFactory
import utopia.vault.nosql.access.ManyIntIdAccess

/**
  * Used for accessing description role ids
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
object DescriptionRoleIds extends ManyIntIdAccess
{
	// COMPUTED	------------------------------
	
	private def factory = DescriptionRoleFactory
	
	
	// IMPLEMENTED	--------------------------
	
	override def target = factory.target
	
	override def table = factory.table
	
	override def globalCondition = None
}
