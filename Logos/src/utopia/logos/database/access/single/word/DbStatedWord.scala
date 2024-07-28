package utopia.logos.database.access.single.word

import utopia.logos.database.factory.word.StatedWordDbFactory
import utopia.logos.database.storable.word.{WordModel, WordPlacementModel}
import utopia.logos.model.combined.word.StatedWord
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual stated words
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object DbStatedWord extends SingleRowModelAccess[StatedWord] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * A database model (factory) used for interacting with linked words
	  */
	protected def model = WordModel
	
	/**
	  * A database model (factory) used for interacting with the linked use case
	  */
	protected def useCaseModel = WordPlacementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StatedWordDbFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted stated word
	  * @return An access point to that stated word
	  */
	def apply(id: Int) = DbSingleStatedWord(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique stated words.
	  * @return An access point to the stated word that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueStatedWordAccess(mergeCondition(condition))
}

