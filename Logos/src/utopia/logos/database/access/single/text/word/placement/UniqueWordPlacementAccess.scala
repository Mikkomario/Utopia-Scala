package utopia.logos.database.access.single.text.word.placement

import utopia.logos.database.access.single.text.placement.UniqueTextPlacementAccessLike
import utopia.logos.database.factory.text.WordPlacementDbFactory
import utopia.logos.database.storable.text.WordPlacementDbModel
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.database.Connection
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object UniqueWordPlacementAccess extends ViewFactory[UniqueWordPlacementAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): UniqueWordPlacementAccess = 
		_UniqueWordPlacementAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueWordPlacementAccess(override val accessCondition: Option[Condition]) 
		extends UniqueWordPlacementAccess
}

/**
  * A common trait for access points that return individual and distinct word placements.
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueWordPlacementAccess 
	extends UniqueTextPlacementAccessLike[WordPlacement, UniqueWordPlacementAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the statement where the referenced word appears. 
	  * None if no word placement (or value) was found.
	  */
	def statementId(implicit connection: Connection) = parentId
	/**
	  * Id of the word that appears in the described statement. 
	  * None if no word placement (or value) was found.
	  */
	def wordId(implicit connection: Connection) = placedId
	/**
	  * Style in which this word is used in this context. 
	  * None if no word placement (or value) was found.
	  */
	def style(implicit connection: Connection) = 
		pullColumn(model.style.column).int.flatMap(DisplayStyle.findForId)
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = WordPlacementDbFactory
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	override protected def model = WordPlacementDbModel
	override protected def self = this
	
	override def apply(condition: Condition): UniqueWordPlacementAccess = UniqueWordPlacementAccess(condition)
}

