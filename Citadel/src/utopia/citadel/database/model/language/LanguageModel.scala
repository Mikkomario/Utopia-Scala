package utopia.citadel.database.model.language

import java.time.Instant
import utopia.citadel.database.factory.language.LanguageFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.language.LanguageData
import utopia.metropolis.model.stored.language.Language
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing LanguageModel instances and for inserting Languages to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object LanguageModel extends DataInserter[LanguageModel, Language, LanguageData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains Language isoCode
	  */
	val isoCodeAttName = "isoCode"
	
	/**
	  * Name of the property that contains Language created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains Language isoCode
	  */
	def isoCodeColumn = table(isoCodeAttName)
	
	/**
	  * Column that contains Language created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = LanguageFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: LanguageData) = apply(None, Some(data.isoCode), Some(data.created))
	
	override def complete(id: Value, data: LanguageData) = Language(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this Language was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A Language id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param isoCode 2 letter ISO-standard code for this language
	  * @return A model containing only the specified isoCode
	  */
	def withIsoCode(isoCode: String) = apply(isoCode = Some(isoCode))
}

/**
  * Used for interacting with Languages in the database
  * @param id Language database id
  * @param isoCode 2 letter ISO-standard code for this language
  * @param created Time when this Language was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class LanguageModel(id: Option[Int] = None, isoCode: Option[String] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[Language]
{
	// IMPLEMENTED	--------------------
	
	override def factory = LanguageModel.factory
	
	override def valueProperties = 
	{
		import LanguageModel._
		Vector("id" -> id, isoCodeAttName -> isoCode, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param isoCode A new isoCode
	  * @return A new copy of this model with the specified isoCode
	  */
	def withIsoCode(isoCode: String) = copy(isoCode = Some(isoCode))
}

