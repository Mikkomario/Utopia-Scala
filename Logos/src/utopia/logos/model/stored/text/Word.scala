package utopia.logos.model.stored.text

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.access.single.text.word.DbSingleWord
import utopia.logos.model.factory.text.WordFactoryWrapper
import utopia.logos.model.partial.text.WordData
import utopia.vault.model.template.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object Word extends StoredFromModelFactory[WordData, Word]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = WordData
	
	override protected def complete(model: AnyModel, data: WordData) =
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a word that has already been stored in the database
  * @param id id of this word in the database
  * @param data Wrapped word data
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class Word(id: Int, data: WordData) 
	extends StoredModelConvertible[WordData] with FromIdFactory[Int, Word] 
		with WordFactoryWrapper[WordData, Word]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this word in the database
	  */
	def access = DbSingleWord(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	override def toString = data.text
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: WordData) = copy(data = data)
}

