package utopia.logos.database.factory.text

import utopia.logos.model.combined.text.StatedWord
import utopia.logos.model.stored.text.{Word, WordPlacement}
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading stated words from the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object StatedWordDbFactory extends CombiningFactory[StatedWord, Word, WordPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = WordPlacementDbFactory
	
	override def parentFactory = WordDbFactory
	
	/**
	  * @param word word to wrap
	  * @param useCase use case to attach to this word
	  */
	override def apply(word: Word, useCase: WordPlacement) = StatedWord(word, useCase)
}

