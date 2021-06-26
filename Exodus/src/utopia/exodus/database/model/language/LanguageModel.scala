package utopia.exodus.database.model.language

import utopia.exodus.database.factory.language.LanguageFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

@deprecated("Please use the Citadel version instead", "v2.0")
object LanguageModel
{
	// ATTRIBUTES	-------------------------------
	
	/**
	  * Name of the attribute that contains the language ISO-code
	  */
	val isoCodeAttName = "isoCode"
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Table this model uses
	  */
	def table = LanguageFactory.table
	
	/**
	  * @return Column that contains the language ISO-code
	  */
	def isoCodeColumn = table(isoCodeAttName)
	
	
	// OTHER	-----------------------------------
	
	/**
	  * @param code Language ISO-code
	  * @return A model with only ISO-code set
	  */
	def withIsoCode(code: String) = apply(isoCode = Some(code))
	
	/**
	  * Inserts a new language to the DB (please make sure no such language exists before inserting one)
	  * @param code Language ISO-code
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted language
	  */
	def insert(code: String)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(code)).insert().getInt
		Language(newId, code)
	}
	
	/**
	  * Reads language from DB or inserts one if it doesn't exist
	  * @param code Language ISO-code
	  * @param connection DB Connection (implicit)
	  * @return Read or created language
	  */
	def getOrInsert(code: String)(implicit connection: Connection) =
	{
		LanguageFactory.get(withIsoCode(code).toCondition).getOrElse(insert(code))
	}
}

/**
  * Used for interacting with language data in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
case class LanguageModel(id: Option[Int] = None, isoCode: Option[String] = None) extends StorableWithFactory[Language]
{
	import LanguageModel._
	
	override def factory = LanguageFactory
	
	override def valueProperties = Vector("id" -> id, isoCodeAttName -> isoCode)
}