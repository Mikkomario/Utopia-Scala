package utopia.logos.database.access.single.url.link.placement

import utopia.logos.database.access.single.text.placement.UniqueTextPlacementAccessLike
import utopia.logos.database.factory.url.LinkPlacementDbFactory
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.database.Connection
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object UniqueLinkPlacementAccess extends ViewFactory[UniqueLinkPlacementAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): UniqueLinkPlacementAccess = 
		_UniqueLinkPlacementAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueLinkPlacementAccess(override val accessCondition: Option[Condition]) 
		extends UniqueLinkPlacementAccess
}

/**
  * A common trait for access points that return individual and distinct link placements.
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueLinkPlacementAccess 
	extends UniqueTextPlacementAccessLike[LinkPlacement, UniqueLinkPlacementAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the statement where the specified link is referenced. 
	  * None if no link placement (or value) was found.
	  */
	def statementId(implicit connection: Connection) = parentId
	/**
	  * Referenced / placed link. 
	  * None if no link placement (or value) was found.
	  */
	def linkId(implicit connection: Connection) = placedId
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkPlacementDbFactory
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	override protected def model = LinkPlacementDbModel
	override protected def self = this
	
	override def apply(condition: Condition): UniqueLinkPlacementAccess = UniqueLinkPlacementAccess(condition)
}

