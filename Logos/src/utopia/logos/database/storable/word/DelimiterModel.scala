package utopia.logos.database.storable.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.word.DelimiterDbFactory
import utopia.logos.model.factory.word.DelimiterFactory
import utopia.logos.model.partial.word.DelimiterData
import utopia.logos.model.stored.word.Delimiter
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.FromIdFactory
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing DelimiterModel instances and for inserting delimiters to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DelimiterModel 
	extends StorableFactory[DelimiterModel, Delimiter, DelimiterData] with DelimiterFactory[DelimiterModel]
		with FromIdFactory[Int, DelimiterModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val text = property("text")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val created = property("created")
	
	/**
	  * Name of the property that contains delimiter text
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val textAttName = "text"
	/**
	  * Name of the property that contains delimiter created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * The factory object used by this model type
	  */
	def factory = DelimiterDbFactory
	
	/**
	  * Column that contains delimiter text
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def textColumn = table(textAttName)
	/**
	  * Column that contains delimiter created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def createdColumn = table(createdAttName)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: DelimiterData) = apply(None, data.text, Some(data.created))
	
	/**
	  * @return A model with that id
	  */
	override def withId(id: Int) = apply(Some(id))
	
	override protected def complete(id: Value, data: DelimiterData) = Delimiter(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this delimiter was added to the database
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param text The characters that form this delimiter
	  * @return A model containing only the specified text
	  */
	def withText(text: String) = apply(text = text)
}

/**
  * Used for interacting with Delimiters in the database
  * @param id delimiter database id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
case class DelimiterModel(id: Option[Int] = None, text: String = "", created: Option[Instant] = None) 
	extends StorableWithFactory[Delimiter] with DelimiterFactory[DelimiterModel] with FromIdFactory[Int, DelimiterModel]
{
	// IMPLEMENTED	--------------------
	
	override def factory = DelimiterModel.factory
	
	override def valueProperties =
		Vector("id" -> id, DelimiterModel.text.name -> text, DelimiterModel.created.name -> created)
	
	override def withId(id: Int): DelimiterModel = copy(id = Some(id))
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this delimiter was added to the database
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param text The characters that form this delimiter
	  * @return A new copy of this model with the specified text
	  */
	def withText(text: String) = copy(text = text)
}

