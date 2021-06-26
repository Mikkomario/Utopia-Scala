package utopia.citadel.database.access.id.many

import utopia.citadel.database.factory.language.LanguageFactory
import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.access.ManyIdAccess

/**
  * An access point to multiple language ids at once
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1
  */
object DbLanguageIds extends ManyIdAccess[Int]
{
	// IMPLEMENTED	--------------------------
	
	override def target = factory.target
	
	override def valueToId(value: Value) = value.int
	
	override def table = factory.table
	
	override val globalCondition = None
	
	
	// COMPUTED	-------------------------------
	
	private def factory = LanguageFactory
}
