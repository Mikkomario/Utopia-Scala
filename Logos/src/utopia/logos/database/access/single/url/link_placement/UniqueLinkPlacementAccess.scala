package utopia.logos.database.access.single.url.link_placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.url.LinkPlacementDbFactory
import utopia.logos.database.storable.url.LinkPlacementModel
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object UniqueLinkPlacementAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueLinkPlacementAccess = new _UniqueLinkPlacementAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueLinkPlacementAccess(condition: Condition) extends UniqueLinkPlacementAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct link placements.
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait UniqueLinkPlacementAccess 
	extends SingleRowModelAccess[LinkPlacement] with FilterableView[UniqueLinkPlacementAccess] 
		with DistinctModelAccess[LinkPlacement, Option[LinkPlacement], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * 
		Id of the statement where the specified link is referenced. None if no link placement (or value) was found.
	  */
	def statementId(implicit connection: Connection) = pullColumn(model.statementId.column).int
	
	/**
	  * Referenced link. None if no link placement (or value) was found.
	  */
	def linkId(implicit connection: Connection) = pullColumn(model.linkId.column).int
	
	/**
	  * 
		Index where the link appears in the statement (0-based). None if no link placement (or value) was found.
	  */
	def orderIndex(implicit connection: Connection) = pullColumn(model.orderIndex.column).int
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LinkPlacementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkPlacementDbFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueLinkPlacementAccess = 
		new UniqueLinkPlacementAccess._UniqueLinkPlacementAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the link ids of the targeted link placements
	  * @param newLinkId A new link id to assign
	  * @return Whether any link placement was affected
	  */
	def linkId_=(newLinkId: Int)(implicit connection: Connection) = putColumn(model.linkId.column, newLinkId)
	
	/**
	  * Updates the order indices of the targeted link placements
	  * @param newOrderIndex A new order index to assign
	  * @return Whether any link placement was affected
	  */
	def orderIndex_=(newOrderIndex: Int)(implicit connection: Connection) = 
		putColumn(model.orderIndex.column, newOrderIndex)
	
	/**
	  * Updates the statement ids of the targeted link placements
	  * @param newStatementId A new statement id to assign
	  * @return Whether any link placement was affected
	  */
	def statementId_=(newStatementId: Int)(implicit connection: Connection) = 
		putColumn(model.statementId.column, newStatementId)
}

