package utopia.logos.database.model.text

import com.vdurmont.emoji.EmojiParser
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.StringExtensions._
import utopia.logos.database.factory.text.WordFactory
import utopia.logos.model.partial.text.WordData
import utopia.logos.model.stored.text.Word
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.time.Instant

/**
  * Used for constructing WordModel instances and for inserting words to the database
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object WordModel extends DataInserter[WordModel, Word, WordData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains word text
	  */
	val textAttName = "text"
	
	/**
	  * Name of the property that contains word created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains word text
	  */
	def textColumn = table(textAttName)
	
	/**
	  * Column that contains word created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = WordFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: WordData) = apply(None, data.text, Some(data.created))
	
	override protected def complete(id: Value, data: WordData) = Word(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this word was added to the database
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A word id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
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
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class WordModel(id: Option[Int] = None, text: String = "", created: Option[Instant] = None) 
	extends StorableWithFactory[Word]
{
	// IMPLEMENTED	--------------------
	
	override def factory = WordModel.factory
	
	override def valueProperties = {
		import WordModel._
		// Converts potential emoji content to aliases before storing them to the database
		val nonEmojiText = text.mapIfNotEmpty(EmojiParser.parseToAliases)
		Vector("id" -> id, textAttName -> nonEmojiText, createdAttName -> created)
	}
	
	
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

