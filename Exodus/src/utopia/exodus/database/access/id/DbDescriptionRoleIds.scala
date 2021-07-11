package utopia.exodus.database.access.id

import utopia.exodus.database.factory.description.DescriptionRoleFactory
import utopia.vault.nosql.access.many.id.ManyIntIdAccess

/**
  * Used for accessing description role ids
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
object DbDescriptionRoleIds extends ManyIntIdAccess
{
	// COMPUTED	------------------------------
	
	private def factory = DescriptionRoleFactory
	
	
	// IMPLEMENTED	--------------------------
	
	override def target = factory.target
	
	override def table = factory.table
	
	override def globalCondition = None
}
