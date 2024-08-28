package utopia.logos.database.access.many.url.link_placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.many.word.statement.ManyStatementPlacedAccess
import utopia.logos.database.factory.url.LinkPlacementDbFactory
import utopia.logos.database.storable.url.LinkPlacementModel
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

@deprecated("Replaced with a new version", "v0.3")
object ManyLinkPlacementsAccess extends ViewFactory[ManyLinkPlacementsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyLinkPlacementsAccess = 
		 new _ManyLinkPlacementsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyLinkPlacementsAccess(condition: Condition) extends ManyLinkPlacementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple link placements at a time
  * @author Mikko Hilpinen
  * @since 16.10.2023, v0.1
  */
@deprecated("Replaced with a new version", "v0.3")
trait ManyLinkPlacementsAccess 
	extends ManyRowModelAccess[LinkPlacement] with Indexed 
		with ManyStatementPlacedAccess[ManyLinkPlacementsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * statement ids of the accessible link placements
	  */
	def statementIds(implicit connection: Connection) = pullColumn(model.statementId.column)
		.map { v => v.getInt }
	
	/**
	  * link ids of the accessible link placements
	  */
	def linkIds(implicit connection: Connection) = pullColumn(model.linkId.column).map { v => v.getInt }
	
	/**
	  * order indices of the accessible link placements
	  */
	def orderIndices(implicit connection: Connection) = pullColumn(model.orderIndex.column)
		.map { v => v.getInt }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkPlacementDbFactory
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	override protected def model = LinkPlacementModel
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyLinkPlacementsAccess = ManyLinkPlacementsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the link ids of the targeted link placements
	  * @param newLinkId A new link id to assign
	  * @return Whether any link placement was affected
	  */
	def linkIds_=(newLinkId: Int)(implicit connection: Connection) = putColumn(model.linkId.column, newLinkId)
	
	/**
	  * @param linkId Id of the targeted link
	  * @return Access to placements of that link
	  */
	def ofLink(linkId: Int) = filter(model.withLinkId(linkId).toCondition)
	
	/**
	  * Updates the order indices of the targeted link placements
	  * @param newOrderIndex A new order index to assign
	  * @return Whether any link placement was affected
	  */
	def orderIndices_=(newOrderIndex: Int)(implicit connection: Connection) = 
		putColumn(model.orderIndex.column, newOrderIndex)
	
	/**
	  * Updates the statement ids of the targeted link placements
	  * @param newStatementId A new statement id to assign
	  * @return Whether any link placement was affected
	  */
	def statementIds_=(newStatementId: Int)(implicit connection: Connection) = 
		putColumn(model.statementId.column, newStatementId)
}

