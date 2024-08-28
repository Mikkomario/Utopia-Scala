package utopia.logos.database.storable.text

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.LogosTables
import utopia.logos.model.factory.text.WordFactory
import utopia.logos.model.partial.text.WordData
import utopia.logos.model.stored.text.Word
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.{FromIdFactory, HasId, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing WordDbModel instances and for inserting words to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object WordDbModel 
	extends StorableFactory[WordDbModel, Word, WordData] with FromIdFactory[Int, WordDbModel] 
		with HasIdProperty with WordFactory[WordDbModel]
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
	
	override def table = LogosTables.word
	
	override def apply(data: WordData) = apply(None, data.text, Some(data.created))
	
	/**
	  * @param created Time when this word was added to the database
	  * @return A model containing only the specified created
	  */
	override def withCreated(created: Instant) = apply(created = Some(created))
	override def withId(id: Int) = apply(id = Some(id))
	/**
	  * @param text Text representation of this word
	  * @return A model containing only the specified text
	  */
	override def withText(text: String) = apply(text = text)
	
	override protected def complete(id: Value, data: WordData) = Word(id.getInt, data)
}

/**
  * Used for interacting with Words in the database
  * @param id word database id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class WordDbModel(id: Option[Int] = None, text: String = "", created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, WordDbModel] 
		with WordFactory[WordDbModel]
{
	// IMPLEMENTED	--------------------
	
	override def table = WordDbModel.table
	
	override def valueProperties = 
		Vector(WordDbModel.id.name -> id, WordDbModel.text.name -> text, WordDbModel.created.name -> created)
	
	/**
	  * @param created Time when this word was added to the database
	  * @return A new copy of this model with the specified created
	  */
	override def withCreated(created: Instant) = copy(created = Some(created))
	override def withId(id: Int) = copy(id = Some(id))
	/**
	  * @param text Text representation of this word
	  * @return A new copy of this model with the specified text
	  */
	override def withText(text: String) = copy(text = text)
}

