package utopia.logos.database.access.many.text.word_placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition
import utopia.logos.database.access.many.text.statement.ManyStatementPlacedAccess
import utopia.logos.database.factory.text.WordPlacementFactory
import utopia.logos.database.model.text.WordPlacementModel
import utopia.logos.model.stored.text.WordPlacement

object ManyWordPlacementsAccess
{
	// NESTED	--------------------
	
	private class ManyWordPlacementsSubView(condition: Condition) extends ManyWordPlacementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple word placements at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait ManyWordPlacementsAccess 
	extends ManyRowModelAccess[WordPlacement] with ManyStatementPlacedAccess[ManyWordPlacementsAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * statement ids of the accessible word placements
	  */
	def statementIds(implicit connection: Connection) = pullColumn(model.statementIdColumn)
		.map { v => v.getInt }
	/**
	  * word ids of the accessible word placements
	  */
	def wordIds(implicit connection: Connection) = pullColumn(model.wordIdColumn).map { v => v.getInt }
	/**
	  * order indexs of the accessible word placements
	  */
	def orderIndices(implicit connection: Connection) = pullColumn(model.orderIndexColumn)
		.map { v => v.getInt }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	
	// IMPLEMENTED	--------------------
	
	/**
	 * Factory used for constructing database the interaction models
	 */
	override protected def model = WordPlacementModel
	
	override def factory = WordPlacementFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyWordPlacementsAccess = 
		new ManyWordPlacementsAccess.ManyWordPlacementsSubView(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	 * @param wordId Id of the linked word
	 * @return Access to placements of that word
	 */
	def ofWord(wordId: Int) = filter(model.withWordId(wordId).toCondition)
	/**
	 * @param wordIds Ids of the targeted words
	 * @return Access to the placements of those words
	 */
	def ofWords(wordIds: Iterable[Int]) = filter(model.wordIdColumn.in(wordIds))
	/**
	 * @param wordId Linked word
	 * @param position Targeted placement / position index
	 * @return Access to that word's placement at that location
	 */
	def ofWordAtPosition(wordId: Int, position: Int) =
		filter(model.withWordId(wordId).withOrderIndex(position).toCondition)
	
	/**
	  * Updates the order indexs of the targeted word placements
	  * @param newOrderIndex A new order index to assign
	  * @return Whether any word placement was affected
	  */
	def orderIndices_=(newOrderIndex: Int)(implicit connection: Connection) =
		putColumn(model.orderIndexColumn, newOrderIndex)
	
	/**
	  * Updates the statement ids of the targeted word placements
	  * @param newStatementId A new statement id to assign
	  * @return Whether any word placement was affected
	  */
	def statementIds_=(newStatementId: Int)(implicit connection: Connection) = 
		putColumn(model.statementIdColumn, newStatementId)
	
	/**
	  * Updates the word ids of the targeted word placements
	  * @param newWordId A new word id to assign
	  * @return Whether any word placement was affected
	  */
	def wordIds_=(newWordId: Int)(implicit connection: Connection) = putColumn(model.wordIdColumn, newWordId)
}

