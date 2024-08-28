package utopia.logos.database.access.single.url.link.placement

import utopia.logos.database.factory.url.LinkPlacementDbFactory
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual link placements
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbLinkPlacement extends SingleRowModelAccess[LinkPlacement] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	private def model = LinkPlacementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkPlacementDbFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted link placement
	  * @return An access point to that link placement
	  */
	def apply(id: Int) = DbSingleLinkPlacement(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique link placements.
	  * @return An access point to the link placement that satisfies the specified condition
	  */
	private def distinct(condition: Condition) = UniqueLinkPlacementAccess(condition)
}

