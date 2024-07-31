package utopia.logos.database.access.many.word.placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.many.word.statement.ManyStatementPlacedAccess
import utopia.logos.database.factory.word.WordPlacementDbFactory
import utopia.logos.database.storable.word.WordPlacementModel
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.stored.word.WordPlacement
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyWordPlacementsAccess extends ViewFactory[ManyWordPlacementsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyWordPlacementsAccess = 
		 new _ManyWordPlacementsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyWordPlacementsAccess(condition: Condition) extends ManyWordPlacementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple word placements at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait ManyWordPlacementsAccess 
	extends ManyRowModelAccess[WordPlacement] with Indexed 
		with ManyStatementPlacedAccess[ManyWordPlacementsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * statement ids of the accessible word placements
	  */
	def statementIds(implicit connection: Connection) = pullColumn(model.statementId.column)
		.map { v => v.getInt }
	
	/**
	  * word ids of the accessible word placements
	  */
	def wordIds(implicit connection: Connection) = pullColumn(model.wordId.column).map { v => v.getInt }
	
	/**
	  * order indices of the accessible word placements
	  */
	def orderIndices(implicit connection: Connection) = pullColumn(model.orderIndex.column)
		.map { v => v.getInt }
	
	/**
	  * styles of the accessible word placements
	  */
	def styles(implicit connection: Connection) = 
		pullColumn(model.style.column).map { v => v.getInt }.flatMap(DisplayStyle.findForId)
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = WordPlacementDbFactory
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	override protected def model = WordPlacementModel
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyWordPlacementsAccess = ManyWordPlacementsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param wordId Id of the linked word
	  * @return Access to placements of that word
	  */
	def ofWord(wordId: Int) = filter(model.withWordId(wordId).toCondition)
	
	/**
	  * @param wordId Linked word
	  * @param position Targeted placement / position index
	  * @return Access to that word's placement at that location
	  */
	def ofWordAtPosition(wordId: Int, position: Int) = 
		filter(model.withWordId(wordId).withOrderIndex(position).toCondition)
	
	/**
	  * @param wordIds Ids of the targeted words
	  * @return Access to the placements of those words
	  */
	def ofWords(wordIds: Iterable[Int]) = filter(model.wordId.column.in(wordIds))
	
	/**
	  * Updates the order indices of the targeted word placements
	  * @param newOrderIndex A new order index to assign
	  * @return Whether any word placement was affected
	  */
	def orderIndices_=(newOrderIndex: Int)(implicit connection: Connection) = 
		putColumn(model.orderIndex.column, newOrderIndex)
	
	/**
	  * Updates the statement ids of the targeted word placements
	  * @param newStatementId A new statement id to assign
	  * @return Whether any word placement was affected
	  */
	def statementIds_=(newStatementId: Int)(implicit connection: Connection) = 
		putColumn(model.statementId.column, newStatementId)
	
	/**
	  * Updates the styles of the targeted word placements
	  * @param newStyle A new style to assign
	  * @return Whether any word placement was affected
	  */
	def styles_=(newStyle: DisplayStyle)(implicit connection: Connection) = 
		putColumn(model.style.column, newStyle.id)
	
	/**
	  * Updates the word ids of the targeted word placements
	  * @param newWordId A new word id to assign
	  * @return Whether any word placement was affected
	  */
	def wordIds_=(newWordId: Int)(implicit connection: Connection) = putColumn(model.wordId.column, newWordId)
}

