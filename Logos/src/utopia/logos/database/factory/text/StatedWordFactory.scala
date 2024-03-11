package utopia.logos.database.factory.text

import utopia.vault.nosql.factory.row.linked.CombiningFactory
import utopia.logos.model.combined.text.StatedWord
import utopia.logos.model.stored.text.{Word, WordPlacement}

/**
  * Used for reading stated words from the database
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object StatedWordFactory extends CombiningFactory[StatedWord, Word, WordPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = WordPlacementFactory
	
	override def parentFactory = WordFactory
	
	override def apply(word: Word, useCase: WordPlacement) = StatedWord(word, useCase)
}

