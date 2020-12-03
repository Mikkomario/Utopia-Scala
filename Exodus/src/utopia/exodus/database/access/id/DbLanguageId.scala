package utopia.exodus.database.access.id

import utopia.exodus.database.factory.language.LanguageFactory
import utopia.exodus.database.model.language.LanguageModel
import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.access.{SingleIdAccess, UniqueAccess}

/**
  * Used for accessing individual language ids
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
object DbLanguageId extends SingleIdAccess[Int]
{
	// IMPLEMENTED	---------------------------
	
	override def target = factory.target
	
	override def valueToId(value: Value) = value.int
	
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
	
	case class IdForCode(languageCode: String) extends SingleIdAccess[Int] with UniqueAccess[Int]
	{
		// IMPLEMENTED	-----------------------
		
		override def condition = model.withIsoCode(languageCode).toCondition
		
		override def target = DbLanguageId.target
		
		override def valueToId(value: Value) = value.int
		
		override def table = DbLanguageId.table
	}
}
