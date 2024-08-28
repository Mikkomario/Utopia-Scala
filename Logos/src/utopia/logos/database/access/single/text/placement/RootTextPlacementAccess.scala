package utopia.logos.database.access.single.text.placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition

/**
  * Common trait for root access points to text placements
  * @tparam A Type of pulled text placements
  * @author Mikko Hilpinen
  * @since 28.08.2024, v0.3
  */
trait RootTextPlacementAccess[+A, +UA] extends SingleModelAccess[A] with Indexed
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Model used for interacting with database properties
	  */
	protected def model: TextPlacementDbProps
	
	/**
	  * @param condition A filter condition applied. Assumed to yield distinct results.
	  * @return Access to the placement which fulfills the specified condition.
	  */
	protected def filterDistinct(condition: Condition): UA
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param id Id of the targeted text placement
	  * @return Access to that placement's data in the DB
	  */
	def apply(id: Int) = filterDistinct(index <=> id)
	
	/**
	  * @param parentId Id of the parent text or entity
	  * @param placedId Id of the placed text or entity
	  * @return Access to a link between those two instances
	  */
	def between(parentId: Int, placedId: Int) =
		filterDistinct(model.parentId <=> parentId && model.placedId <=> placedId)
	
	/**
	  * @param parentId Id of the targeted parent text
	  * @param orderIndex Position where the placed text appears
	  * @return Access to that placement
	  */
	def at(parentId: Int, orderIndex: Int) =
		filterDistinct(model.parentId <=> parentId && model.orderIndex <=> orderIndex)
}
