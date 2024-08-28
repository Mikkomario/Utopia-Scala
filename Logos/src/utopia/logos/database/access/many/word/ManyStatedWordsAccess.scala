package utopia.logos.database.access.many.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.word.StatedWordDbFactory
import utopia.logos.database.storable.word.WordPlacementModel
import utopia.logos.model.combined.word.StatedWord
import utopia.logos.model.enumeration.DisplayStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

@deprecated("Replaced with a new version", "v0.3")
object ManyStatedWordsAccess extends ViewFactory[ManyStatedWordsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyStatedWordsAccess = new _ManyStatedWordsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyStatedWordsAccess(condition: Condition) extends ManyStatedWordsAccess
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
@deprecated("Replaced with a new version", "v0.3")
trait ManyStatedWordsAccess 
	extends ManyWordsAccessLike[StatedWord, ManyStatedWordsAccess] with ManyRowModelAccess[StatedWord]
{
	// COMPUTED	--------------------
	
	/**
	  * statement ids of the accessible word placements
	  */
	def useCaseStatementIds(implicit connection: Connection) = 
		pullColumn(useCaseModel.statementId.column).map { v => v.getInt }
	
	/**
	  * word ids of the accessible word placements
	  */
	def useCaseWordIds(implicit connection: Connection) = 
		pullColumn(useCaseModel.wordId.column).map { v => v.getInt }
	
	/**
	  * order indices of the accessible word placements
	  */
	def useCaseOrderIndices(implicit connection: Connection) = 
		pullColumn(useCaseModel.orderIndex.column).map { v => v.getInt }
	
	/**
	  * styles of the accessible word placements
	  */
	def useCaseStyles(implicit connection: Connection) = 
		pullColumn(useCaseModel.style.column).map { v => v.getInt }.flatMap(DisplayStyle.findForId)
	
	/**
	  * Model (factory) used for interacting the word placements associated with this stated word
	  */
	protected def useCaseModel = WordPlacementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StatedWordDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyStatedWordsAccess = ManyStatedWordsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the order indices of the targeted word placements
	  * @param newOrderIndex A new order index to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseOrderIndices_=(newOrderIndex: Int)(implicit connection: Connection) = 
		putColumn(useCaseModel.orderIndex.column, newOrderIndex)
	
	/**
	  * Updates the statement ids of the targeted word placements
	  * @param newStatementId A new statement id to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseStatementIds_=(newStatementId: Int)(implicit connection: Connection) = 
		putColumn(useCaseModel.statementId.column, newStatementId)
	
	/**
	  * Updates the styles of the targeted word placements
	  * @param newStyle A new style to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseStyles_=(newStyle: DisplayStyle)(implicit connection: Connection) = 
		putColumn(useCaseModel.style.column, newStyle.id)
	
	/**
	  * Updates the word ids of the targeted word placements
	  * @param newWordId A new word id to assign
	  * @return Whether any word placement was affected
	  */
	def useCaseWordIds_=(newWordId: Int)(implicit connection: Connection) = 
		putColumn(useCaseModel.wordId.column, newWordId)
}

