package utopia.citadel.database.access.id.many

import utopia.citadel.database.factory.language.LanguageFactory
import utopia.vault.nosql.access.many.column.ManyIntIdAccess

/**
  * An access point to multiple language ids at once
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1.0
  */
object DbLanguageIds extends ManyIntIdAccess
{
	// IMPLEMENTED	--------------------------
	
	override def target = factory.target
	
	override def table = factory.table
	
	override val accessCondition = None
	
	
	// COMPUTED	-------------------------------
	
	private def factory = LanguageFactory
}
