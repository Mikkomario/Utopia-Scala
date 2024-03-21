package utopia.logos.database.storable.word

import com.vdurmont.emoji.EmojiParser
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.StringExtensions._
import utopia.logos.database.factory.word.WordDbFactory
import utopia.logos.model.factory.word.WordFactory
import utopia.logos.model.partial.word.WordData
import utopia.logos.model.stored.word.Word
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.FromIdFactory
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing WordModel instances and for inserting words to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object WordModel extends StorableFactory[WordModel, Word, WordData]
	with WordFactory[WordModel] with FromIdFactory[Int, WordModel]
{
	// ATTRIBUTES	--------------------
	
	lazy val text = property("text")
	lazy val created = property("created")
	
	/**
	  * Name of the property that contains word text
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val textAttName = "text"
	
	/**
	  * Name of the property that contains word created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * The factory object used by this model type
	  */
	def factory = WordDbFactory
	
	/**
	  * Column that contains word text
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def textColumn = table(textAttName)
	/**
	  * Column that contains word created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def createdColumn = table(createdAttName)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: WordData) = apply(None, data.text, Some(data.created))
	
	/**
	  * @return A model with that id
	  */
	override def withId(id: Int) = apply(Some(id))
	
	override protected def complete(id: Value, data: WordData) = Word(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this word was added to the database
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param text Text representation of this word
	  * @return A model containing only the specified text
	  */
	def withText(text: String) = apply(text = text)
}

/**
  * Used for interacting with Words in the database
  * @param id word database id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
case class WordModel(id: Option[Int] = None, text: String = "", created: Option[Instant] = None) 
	extends StorableWithFactory[Word] with WordFactory[WordModel] with FromIdFactory[Int, WordModel]
{
	// IMPLEMENTED	--------------------
	
	override def factory = WordModel.factory
	
	override def valueProperties = {
		// Converts potential emoji content to aliases before storing them to the database
		val nonEmojiText = text.mapIfNotEmpty(EmojiParser.parseToAliases)
		Vector("id" -> id, WordModel.text.name -> nonEmojiText, WordModel.created.name -> created)
	}
	
	override def withId(id: Int): WordModel = copy(id = Some(id))
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this word was added to the database
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param text Text representation of this word
	  * @return A new copy of this model with the specified text
	  */
	def withText(text: String) = copy(text = text)
}

