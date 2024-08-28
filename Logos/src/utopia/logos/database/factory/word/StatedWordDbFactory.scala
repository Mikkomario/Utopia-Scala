package utopia.logos.database.factory.word

import utopia.logos.model.combined.word.StatedWord
import utopia.logos.model.stored.word.{Word, WordPlacement}
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading stated words from the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with a new version", "v0.3")
object StatedWordDbFactory extends CombiningFactory[StatedWord, Word, WordPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = WordPlacementDbFactory
	
	override def parentFactory = WordDbFactory
	
	override def apply(word: Word, useCase: WordPlacement) = StatedWord(word, useCase)
}

