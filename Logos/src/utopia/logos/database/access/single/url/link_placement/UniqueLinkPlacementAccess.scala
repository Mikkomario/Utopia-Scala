package utopia.logos.database.access.single.url.link_placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition
import utopia.logos.database.factory.url.LinkPlacementFactory
import utopia.logos.database.model.url.LinkPlacementModel
import utopia.logos.model.stored.url.LinkPlacement

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
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
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
	def statementId(implicit connection: Connection) = pullColumn(model.statementIdColumn).int
	
	/**
	  * Referenced link. None if no link placement (or value) was found.
	  */
	def linkId(implicit connection: Connection) = pullColumn(model.linkIdColumn).int
	
	/**
	  * 
		Index where the link appears in the statement (0-based). None if no link placement (or value) was found.
	  */
	def orderIndex(implicit connection: Connection) = pullColumn(model.orderIndexColumn).int
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LinkPlacementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkPlacementFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueLinkPlacementAccess = 
		new UniqueLinkPlacementAccess._UniqueLinkPlacementAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the link ids of the targeted link placements
	  * @param newLinkId A new link id to assign
	  * @return Whether any link placement was affected
	  */
	def linkId_=(newLinkId: Int)(implicit connection: Connection) = putColumn(model.linkIdColumn, newLinkId)
	
	/**
	  * Updates the order indices of the targeted link placements
	  * @param newOrderIndex A new order index to assign
	  * @return Whether any link placement was affected
	  */
	def orderIndex_=(newOrderIndex: Int)(implicit connection: Connection) = 
		putColumn(model.orderIndexColumn, newOrderIndex)
	
	/**
	  * Updates the statement ids of the targeted link placements
	  * @param newStatementId A new statement id to assign
	  * @return Whether any link placement was affected
	  */
	def statementId_=(newStatementId: Int)(implicit connection: Connection) = 
		putColumn(model.statementIdColumn, newStatementId)
}

