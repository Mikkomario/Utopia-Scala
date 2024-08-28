package utopia.logos.model.combined.word

import utopia.flow.view.template.Extender
import utopia.logos.model.partial.word.WordData
import utopia.logos.model.stored.word.{Word, WordPlacement}

/**
  * Represents a word used in a specific statement
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
case class StatedWord(word: Word, useCase: WordPlacement) extends Extender[WordData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this word in the database
	  */
	def id = word.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = word.data
}

