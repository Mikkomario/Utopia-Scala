package utopia.logos.database.factory.text

import com.vdurmont.emoji.EmojiParser
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.StringExtensions._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.logos.database.EmissaryTables
import utopia.logos.model.partial.text.WordData
import utopia.logos.model.stored.text.Word

/**
  * Used for reading word data from the DB
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object WordFactory extends FromValidatedRowModelFactory[Word]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = EmissaryTables.word
	
	// Processes emoji content
	override protected def fromValidatedModel(valid: Model) = 
		Word(valid("id").getInt, WordData(valid("text").getString.mapIfNotEmpty(EmojiParser.parseToUnicode),
			valid("created").getInstant))
}

