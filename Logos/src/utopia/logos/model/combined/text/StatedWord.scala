package utopia.logos.model.combined.text

import utopia.flow.view.template.Extender
import utopia.logos.model.partial.text.WordData
import utopia.logos.model.stored.text.{Word, WordPlacement}

/**
  * Represents a word used in a specific statement
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
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

