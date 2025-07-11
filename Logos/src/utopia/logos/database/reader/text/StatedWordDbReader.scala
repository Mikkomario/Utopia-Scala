package utopia.logos.database.reader.text

import utopia.logos.model.combined.text.StatedWord
import utopia.logos.model.stored.text.{StoredWord, WordPlacement}
import utopia.vault.nosql.read.linked.CombiningDbRowReader

/**
  * Used for reading stated words from the database
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
object StatedWordDbReader 
	extends CombiningDbRowReader[StoredWord, WordPlacement, StatedWord](WordDbReader, WordPlacementDbReader)
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param word    word to wrap
	  * @param useCase use case to attach to this word
	  */
	override def combine(word: StoredWord, useCase: WordPlacement) = StatedWord(word, useCase)
}

