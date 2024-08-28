package utopia.logos.database.access.single.text.word

import utopia.logos.database.factory.text.StatedWordDbFactory
import utopia.logos.database.storable.text.WordPlacementDbModel
import utopia.logos.model.combined.text.StatedWord
import utopia.logos.model.enumeration.DisplayStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object UniqueStatedWordAccess extends ViewFactory[UniqueStatedWordAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override
		 def apply(condition: Condition): UniqueStatedWordAccess = _UniqueStatedWordAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueStatedWordAccess(override val accessCondition: Option[Condition]) 
		extends UniqueStatedWordAccess
}

/**
  * A common trait for access points that return distinct stated words
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueStatedWordAccess 
	extends UniqueWordAccessLike[StatedWord, UniqueStatedWordAccess] with SingleRowModelAccess[StatedWord]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the statement where the referenced word appears. 
	  * None if no word placement (or value) was found.
	  */
	def useCaseStatementId(implicit connection: Connection) = pullColumn(useCaseModel.statementId.column).int
	
	/**
	  * Id of the word that appears in the described statement. 
	  * None if no word placement (or value) was found.
	  */
	def useCaseWordId(implicit connection: Connection) = pullColumn(useCaseModel.wordId.column).int
	
	/**
	  * 0-based index that indicates the specific location of the placed text. 
	  * None if no word placement (or value) was found.
	  */
	def useCaseOrderIndex(implicit connection: Connection) = pullColumn(useCaseModel.orderIndex.column).int
	
	/**
	  * Style in which this word is used in this context. 
	  * None if no word placement (or value) was found.
	  */
	def useCaseStyle(implicit connection: Connection) = 
		pullColumn(useCaseModel.style.column).int.flatMap(DisplayStyle.findForId)
	
	/**
	  * A database model (factory) used for interacting with the linked use case
	  */
	protected def useCaseModel = WordPlacementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StatedWordDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueStatedWordAccess = UniqueStatedWordAccess(condition)
}

