package utopia.logos.database.access.many.text.word

import utopia.logos.database.factory.text.StatedWordDbFactory
import utopia.logos.database.storable.text.WordPlacementDbModel
import utopia.logos.model.combined.text.StatedWord
import utopia.logos.model.enumeration.DisplayStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyStatedWordsAccess extends ViewFactory[ManyStatedWordsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyStatedWordsAccess = _ManyStatedWordsAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _ManyStatedWordsAccess(override val accessCondition: Option[Condition]) 
		extends ManyStatedWordsAccess
}

/**
  * A common trait for access points that return multiple stated words at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024
  */
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
	protected def useCaseModel = WordPlacementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StatedWordDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyStatedWordsAccess = ManyStatedWordsAccess(condition)
}

