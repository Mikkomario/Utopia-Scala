package utopia.logos.database.access.single.url.link_placement

import utopia.logos.database.factory.url.LinkPlacementDbFactory
import utopia.logos.database.storable.url.LinkPlacementModel
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual link placements
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DbLinkPlacement extends SingleRowModelAccess[LinkPlacement] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LinkPlacementModel
	
	
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
	protected def filterDistinct(condition: Condition) = UniqueLinkPlacementAccess(mergeCondition(condition))
}

