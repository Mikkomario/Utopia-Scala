package utopia.logos.model.combined.text

import utopia.flow.view.template.Extender
import utopia.logos.model.factory.text.WordFactoryWrapper
import utopia.logos.model.partial.text.WordData
import utopia.logos.model.stored.text.{Word, WordPlacement}
import utopia.vault.model.template.HasId

object StatedWord
{
	// OTHER	--------------------
	
	/**
	  * @param word word to wrap
	  * @param useCase use case to attach to this word
	  * @return Combination of the specified word and use case
	  */
	def apply(word: Word, useCase: WordPlacement): StatedWord = _StatedWord(word, useCase)
	
	
	// NESTED	--------------------
	
	/**
	  * @param word word to wrap
	  * @param useCase use case to attach to this word
	  */
	private case class _StatedWord(word: Word, useCase: WordPlacement) extends StatedWord
	{
		// IMPLEMENTED	--------------------
		
		override protected def wrap(factory: Word) = copy(word = factory)
	}
}

/**
  * Represents a word used in a specific statement
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait StatedWord extends Extender[WordData] with HasId[Int] with WordFactoryWrapper[Word, StatedWord]
{
	// ABSTRACT	--------------------
	
	/**
	  * Wrapped word
	  */
	def word: Word
	/**
	  * The use case that is attached to this word
	  */
	def useCase: WordPlacement
	
	
	// IMPLEMENTED	--------------------
	
	/**
	  * Id of this word in the database
	  */
	override def id = word.id
	
	override def wrapped = word.data
	override protected def wrappedFactory = word
}

