package utopia.logos.database.access.many.text.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition
import utopia.logos.database.factory.text.StatedWordFactory
import utopia.logos.database.model.text.WordPlacementModel
import utopia.logos.model.combined.text.StatedWord

object ManyStatedWordsAccess
{
	// NESTED	--------------------
	
	private class SubAccess(condition: Condition) extends ManyStatedWordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return multiple stated words at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023
  */
trait ManyStatedWordsAccess 
	extends ManyWordsAccessLike[StatedWord, ManyStatedWordsAccess] with ManyRowModelAccess[StatedWord]
{
	// COMPUTED	--------------------
	
	/**
	  * statement ids of the accessible word placements
	  */
	def useCaseStatementIds(implicit connection: Connection) = 
		pullColumn(useCaseModel.statementIdColumn).map { v => v.getInt }
	
	/**
	  * word ids of the accessible word placements
	  */
	def useCaseWordIds(implicit connection: Connection) = 
		pullColumn(useCaseModel.wordIdColumn).map { v => v.getInt }
	
	/**
	  * order indices of the accessible word placements
	  */
	def useCaseOrderIndices(implicit connection: Connection) =
		pullColumn(useCaseModel.orderIndexColumn).map { v => v.getInt }
	
	/**
	  * Model (factory) used for interacting the word placements associated with this stated word
	  */
	protected def useCaseModel = WordPlacementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StatedWordFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyStatedWordsAccess = 
		new ManyStatedWordsAccess.SubAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the order indexs of the targeted word placements
	  * @param newOrderIndex A new order index to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseOrderIndices_=(newOrderIndex: Int)(implicit connection: Connection) =
		putColumn(useCaseModel.orderIndexColumn, newOrderIndex)
	
	/**
	  * Updates the statement ids of the targeted word placements
	  * @param newStatementId A new statement id to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseStatementIds_=(newStatementId: Int)(implicit connection: Connection) = 
		putColumn(useCaseModel.statementIdColumn, newStatementId)
	
	/**
	  * Updates the word ids of the targeted word placements
	  * @param newWordId A new word id to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseWordIds_=(newWordId: Int)(implicit connection: Connection) = 
		putColumn(useCaseModel.wordIdColumn, newWordId)
}

