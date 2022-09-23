package utopia.citadel.database.access.id.single

import utopia.citadel.database.factory.language.LanguageFactory
import utopia.citadel.database.model.language.LanguageModel
import utopia.flow.collection.value.typeless.Value
import utopia.metropolis.model.partial.language.LanguageData
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.column.{SingleIntIdAccess, UniqueIdAccess}

/**
  * Used for accessing individual language ids
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1.0
  */
object DbLanguageId extends SingleIntIdAccess
{
	// IMPLEMENTED	---------------------------
	
	override def target = factory.target
	
	override def table = factory.table
	
	override def globalCondition = None
	
	
	// COMPUTED	-------------------------------
	
	private def factory = LanguageFactory
	
	private def model = LanguageModel
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param languageCode Targeted language's ISO-code
	  * @return An access point to that language's id
	  */
	def forIsoCode(languageCode: String) = IdForCode(languageCode)
	
	
	// NESTED	-------------------------------
	
	case class IdForCode(languageCode: String) extends UniqueIdAccess[Int]
	{
		// COMPUTED ---------------------------
		
		/**
		 * @param connection Implicit DB Connection
		 * @return This language code id, or a new one based on an insert
		 */
		def getOrInsert()(implicit connection: Connection) =
			pull.getOrElse { LanguageModel.insert(LanguageData(languageCode)).id }
		
		
		// IMPLEMENTED	-----------------------
		
		override def condition = model.withIsoCode(languageCode).toCondition
		
		override def target = DbLanguageId.target
		
		override def table = DbLanguageId.table
		
		override def parseValue(value: Value) = value.int
	}
}
