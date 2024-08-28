package utopia.logos.database.access.many.url.link.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.many.text.placement.ManyTextPlacementsAccessLike
import utopia.logos.database.factory.url.LinkPlacementDbFactory
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyLinkPlacementsAccess extends ViewFactory[ManyLinkPlacementsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyLinkPlacementsAccess = 
		_ManyLinkPlacementsAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _ManyLinkPlacementsAccess(override val accessCondition: Option[Condition]) 
		extends ManyLinkPlacementsAccess
}

/**
  * A common trait for access points which target multiple link placements at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait ManyLinkPlacementsAccess 
	extends ManyTextPlacementsAccessLike[LinkPlacement, ManyLinkPlacementsAccess] 
		with ManyRowModelAccess[LinkPlacement]
{
	// COMPUTED	--------------------
	
	/**
	  * statement ids of the accessible link placements
	  */
	def statementIds(implicit connection: Connection) = parentIds
	
	/**
	  * link ids of the accessible link placements
	  */
	def linkIds(implicit connection: Connection) = placedIds
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkPlacementDbFactory
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	override protected def model = LinkPlacementDbModel
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyLinkPlacementsAccess = ManyLinkPlacementsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param linkId link id to target
	  * @return Copy of this access point that only includes link placements with the specified link id
	  */
	def placingLink(linkId: Int) = filter(model.linkId.column <=> linkId)
	
	/**
	  * @param linkIds Targeted link ids
	  * @return Copy of this access point that only includes link placements where link id is within the
	  *  specified value set
	  */
	def placingLinks(linkIds: IterableOnce[Int]) = filter(model.linkId.column.in(IntSet.from(linkIds)))
	
	/**
	  * @param statementId statement id to target
	  * @return Copy of this access point that only includes link placements with the specified statement id
	  */
	def withinStatement(statementId: Int) = filter(model.statementId.column <=> statementId)
	
	/**
	  * @param statementIds Targeted statement ids
	  * @return Copy of this access point that only includes link placements where statement id is within the
	  *  specified value set
	  */
	def withinStatements(statementIds: IterableOnce[Int]) = 
		filter(model.statementId.column.in(IntSet.from(statementIds)))
}

