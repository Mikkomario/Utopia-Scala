package utopia.logos.database.access.many.text.word.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.many.text.placement.ManyTextPlacementsAccessLike
import utopia.logos.database.factory.text.WordPlacementDbFactory
import utopia.logos.database.storable.text.WordPlacementDbModel
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
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
		_ManyWordPlacementsAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _ManyWordPlacementsAccess(override val accessCondition: Option[Condition]) 
		extends ManyWordPlacementsAccess
}

/**
  * A common trait for access points which target multiple word placements at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait ManyWordPlacementsAccess 
	extends ManyTextPlacementsAccessLike[WordPlacement, ManyWordPlacementsAccess] 
		with ManyRowModelAccess[WordPlacement]
{
	// COMPUTED	--------------------
	
	/**
	  * statement ids of the accessible word placements
	  */
	def statementIds(implicit connection: Connection) = parentIds
	/**
	  * word ids of the accessible word placements
	  */
	def wordIds(implicit connection: Connection) = placedIds
	/**
	  * styles of the accessible word placements
	  */
	def styles(implicit connection: Connection) = 
		pullColumn(model.style.column).map { v => v.getInt }.flatMap(DisplayStyle.findForId)
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = WordPlacementDbFactory
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	override protected def model = WordPlacementDbModel
	override protected def self = this
	
	override def apply(condition: Condition): ManyWordPlacementsAccess = ManyWordPlacementsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param wordId word id to target
	  * @return Copy of this access point that only includes word placements with the specified word id
	  */
	def placingWord(wordId: Int) = filter(model.wordId.column <=> wordId)
	/**
	  * @param wordIds Targeted word ids
	  * @return Copy of this access point that only includes word placements where word id is within the
	  *  specified value set
	  */
	def placingWords(wordIds: IterableOnce[Int]) = filter(model.wordId.column.in(IntSet.from(wordIds)))
	
	/**
	  * @param wordId Linked word
	  * @param position Targeted placement / position index
	  * @return Access to that word's placement at that location
	  */
	def placingWordAtPosition(wordId: Int, position: Int) =
		filter(model.withWordId(wordId).withOrderIndex(position).toCondition)
	
	/**
	 * @param style Targeted display style
	 * @return Access to placements applying the specified display style
	 */
	def withStyle(style: DisplayStyle) = filter(model.style <=> style)
	
	/**
	  * @param statementId statement id to target
	  * @return Copy of this access point that only includes word placements with the specified statement id
	  */
	def withinStatement(statementId: Int) = filter(model.statementId.column <=> statementId)
	/**
	  * @param statementIds Targeted statement ids
	  * @return Copy of this access point that only includes word placements where statement id is within the
	  *  specified value set
	  */
	def withinStatements(statementIds: IterableOnce[Int]) = 
		filter(model.statementId.column.in(IntSet.from(statementIds)))
}

