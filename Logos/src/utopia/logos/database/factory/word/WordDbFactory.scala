package utopia.logos.database.factory.word

import com.vdurmont.emoji.EmojiParser
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.StringExtensions._
import utopia.logos.database.LogosTables
import utopia.logos.model.partial.word.WordData
import utopia.logos.model.stored.word.Word
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading word data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object WordDbFactory extends FromValidatedRowModelFactory[Word]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = LogosTables.word
	
	// Processes emoji content
	override protected def fromValidatedModel(valid: Model) =
		Word(valid("id").getInt,
			WordData(valid("text").getString.mapIfNotEmpty(EmojiParser.parseToUnicode), valid("created").getInstant))
}

