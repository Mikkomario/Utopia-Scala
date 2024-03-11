package utopia.logos.database.access.single.url.link_placement

import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition
import utopia.logos.database.factory.url.LinkPlacementFactory
import utopia.logos.database.model.url.LinkPlacementModel
import utopia.logos.model.stored.url.LinkPlacement

/**
  * Used for accessing individual link placements
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object DbLinkPlacement extends SingleRowModelAccess[LinkPlacement] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LinkPlacementModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkPlacementFactory
	
	
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

