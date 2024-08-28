package utopia.logos.database.storable.text

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.LogosTables
import utopia.logos.model.factory.text.DelimiterFactory
import utopia.logos.model.partial.text.DelimiterData
import utopia.logos.model.stored.text.Delimiter
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.{FromIdFactory, HasId, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing DelimiterDbModel instances and for inserting delimiters to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DelimiterDbModel 
	extends StorableFactory[DelimiterDbModel, Delimiter, DelimiterData] 
		with FromIdFactory[Int, DelimiterDbModel] with HasIdProperty with DelimiterFactory[DelimiterDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with text
	  */
	lazy val text = property("text")
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = LogosTables.delimiter
	
	override def apply(data: DelimiterData) = apply(None, data.text, Some(data.created))
	
	/**
	  * @param created Time when this delimiter was added to the database
	  * @return A model containing only the specified created
	  */
	override def withCreated(created: Instant) = apply(created = Some(created))
	override def withId(id: Int) = apply(id = Some(id))
	/**
	  * @param text The characters that form this delimiter
	  * @return A model containing only the specified text
	  */
	override def withText(text: String) = apply(text = text)
	
	override protected def complete(id: Value, data: DelimiterData) = Delimiter(id.getInt, data)
}

/**
  * Used for interacting with Delimiters in the database
  * @param id delimiter database id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DelimiterDbModel(id: Option[Int] = None, text: String = "", created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, DelimiterDbModel] 
		with DelimiterFactory[DelimiterDbModel]
{
	// IMPLEMENTED	--------------------
	
	override def table = DelimiterDbModel.table
	
	override def valueProperties = 
		Vector(DelimiterDbModel.id.name -> id, DelimiterDbModel.text.name -> text, 
			DelimiterDbModel.created.name -> created)
	
	/**
	  * @param created Time when this delimiter was added to the database
	  * @return A new copy of this model with the specified created
	  */
	override def withCreated(created: Instant) = copy(created = Some(created))
	override def withId(id: Int) = copy(id = Some(id))
	/**
	  * @param text The characters that form this delimiter
	  * @return A new copy of this model with the specified text
	  */
	override def withText(text: String) = copy(text = text)
}

