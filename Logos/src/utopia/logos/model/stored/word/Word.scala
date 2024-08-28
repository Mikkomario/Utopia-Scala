package utopia.logos.model.stored.word

import utopia.logos.database.access.single.word.DbSingleWord
import utopia.logos.model.factory.word.WordFactory
import utopia.logos.model.partial.word.WordData
import utopia.vault.model.template.{FromIdFactory, StoredModelConvertible}

import java.time.Instant

/**
  * Represents a word that has already been stored in the database
  * @param id id of this word in the database
  * @param data Wrapped word data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
case class Word(id: Int, data: WordData) 
	extends StoredModelConvertible[WordData] with WordFactory[Word] with FromIdFactory[Int, Word]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this word in the database
	  */
	def access = DbSingleWord(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def toString = data.text
	
	override def withCreated(created: Instant) = copy(data = data.withCreated(created))
	
	override def withId(id: Int) = copy(id = id)
	
	override def withText(text: String) = copy(data = data.withText(text))
}

