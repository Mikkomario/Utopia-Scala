package utopia.logos.database.access.single.text.word_placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition
import utopia.logos.database.factory.text.WordPlacementFactory
import utopia.logos.database.model.text.WordPlacementModel
import utopia.logos.model.stored.text.WordPlacement

object UniqueWordPlacementAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueWordPlacementAccess = new _UniqueWordPlacementAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueWordPlacementAccess(condition: Condition) extends UniqueWordPlacementAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct word placements.
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait UniqueWordPlacementAccess 
	extends SingleRowModelAccess[WordPlacement] with FilterableView[UniqueWordPlacementAccess] 
		with DistinctModelAccess[WordPlacement, Option[WordPlacement], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the statement where the referenced word appears. None if no word placement (or value) was found.
	  */
	def statementId(implicit connection: Connection) = pullColumn(model.statementIdColumn).int
	
	/**
	  * 
		Id of the word that appears in the described statement. None if no word placement (or value) was found.
	  */
	def wordId(implicit connection: Connection) = pullColumn(model.wordIdColumn).int
	
	/**
	  * Index at which the specified word appears within the referenced statement (0-based). None if
	  *  no word placement (or value) was found.
	  */
	def orderIndex(implicit connection: Connection) = pullColumn(model.orderIndexColumn).int
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = WordPlacementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = WordPlacementFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueWordPlacementAccess = 
		new UniqueWordPlacementAccess._UniqueWordPlacementAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the order indexs of the targeted word placements
	  * @param newOrderIndex A new order index to assign
	  * @return Whether any word placement was affected
	  */
	def orderIndex_=(newOrderIndex: Int)(implicit connection: Connection) = 
		putColumn(model.orderIndexColumn, newOrderIndex)
	
	/**
	  * Updates the statement ids of the targeted word placements
	  * @param newStatementId A new statement id to assign
	  * @return Whether any word placement was affected
	  */
	def statementId_=(newStatementId: Int)(implicit connection: Connection) = 
		putColumn(model.statementIdColumn, newStatementId)
	
	/**
	  * Updates the word ids of the targeted word placements
	  * @param newWordId A new word id to assign
	  * @return Whether any word placement was affected
	  */
	def wordId_=(newWordId: Int)(implicit connection: Connection) = putColumn(model.wordIdColumn, newWordId)
}

