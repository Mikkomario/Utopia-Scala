package utopia.logos.model.stored.text

import utopia.logos.database.access.single.text.word.DbSingleWord
import utopia.logos.model.partial.text.WordData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a word that has already been stored in the database
  * @param id id of this word in the database
  * @param data Wrapped word data
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class Word(id: Int, data: WordData) extends StoredModelConvertible[WordData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this word in the database
	  */
	def access = DbSingleWord(id)
	
	
	// IMPLEMENTED  ----------------
	
	override def toString = data.text
}

