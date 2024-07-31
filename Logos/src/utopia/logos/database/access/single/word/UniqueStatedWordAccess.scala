package utopia.logos.database.access.single.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.word.StatedWordDbFactory
import utopia.logos.database.storable.word.WordPlacementModel
import utopia.logos.model.combined.word.StatedWord
import utopia.logos.model.enumeration.DisplayStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object UniqueStatedWordAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueStatedWordAccess = new _UniqueStatedWordAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueStatedWordAccess(condition: Condition) extends UniqueStatedWordAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return distinct stated words
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait UniqueStatedWordAccess 
	extends UniqueWordAccessLike[StatedWord] with SingleRowModelAccess[StatedWord] 
		with FilterableView[UniqueStatedWordAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the statement where the referenced word appears. None if no word placement (or value) was found.
	  */
	def useCaseStatementId(implicit connection: Connection) = pullColumn(useCaseModel.statementId.column).int
	
	/**
	  * 
		Id of the word that appears in the described statement. None if no word placement (or value) was found.
	  */
	def useCaseWordId(implicit connection: Connection) = pullColumn(useCaseModel.wordId.column).int
	
	/**
	  * Index at which the specified word appears within the referenced statement (0-based). None if
	  * no word placement (or value) was found.
	  */
	def useCaseOrderIndex(implicit connection: Connection) = pullColumn(useCaseModel.orderIndex.column).int
	
	/**
	  * Style in which this word is used in this context. None if no word placement (or value) was found.
	  */
	def useCaseStyle(implicit connection: Connection) = 
		pullColumn(useCaseModel.style.column).int.flatMap(DisplayStyle.findForId)
	
	/**
	  * A database model (factory) used for interacting with the linked use case
	  */
	protected def useCaseModel = WordPlacementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StatedWordDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueStatedWordAccess = UniqueStatedWordAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the order indices of the targeted word placements
	  * @param newOrderIndex A new order index to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseOrderIndex_=(newOrderIndex: Int)(implicit connection: Connection) = 
		putColumn(useCaseModel.orderIndex.column, newOrderIndex)
	
	/**
	  * Updates the statement ids of the targeted word placements
	  * @param newStatementId A new statement id to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseStatementId_=(newStatementId: Int)(implicit connection: Connection) = 
		putColumn(useCaseModel.statementId.column, newStatementId)
	
	/**
	  * Updates the styles of the targeted word placements
	  * @param newStyle A new style to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseStyle_=(newStyle: DisplayStyle)(implicit connection: Connection) = 
		putColumn(useCaseModel.style.column, newStyle.id)
	
	/**
	  * Updates the word ids of the targeted word placements
	  * @param newWordId A new word id to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseWordId_=(newWordId: Int)(implicit connection: Connection) = 
		putColumn(useCaseModel.wordId.column, newWordId)
}

