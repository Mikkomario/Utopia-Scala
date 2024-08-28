package utopia.logos.database.access.many.text.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.template.PlacedAccessLike
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.many.model.ManyModelAccess

/**
  * Common trait for access points which yield multiple placed texts of some kind (including their placement links)
  * at once.
  * @tparam A Type of read placed texts
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 28.08.2024, v0.3
  */
trait ManyPlacedTextAccessLike[+A, +Repr] extends ManyModelAccess[A] with PlacedAccessLike[Repr]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Access to text placement database properties
	  */
	protected def placementModel: TextPlacementDbProps
	
	
	// COMPUTED -----------------------
	
	/**
	  * @param connection Implicit DB connection
	  * @return Ids of the linked parent texts
	  */
	def parentIds(implicit connection: Connection) = pullColumn(placementModel.parentId)
	
	
	// IMPLEMENTED  -------------------
	
	override protected def orderIndexColumn: Column = placementModel.orderIndex
	
	
	// OTHER    -----------------------
	
	/**
	  * @param parentId parent id to target
	  * @return Copy of this access point that only includes text placements with the specified parent id
	  */
	def withinText(parentId: Int) = filter(placementModel.parentId.column <=> parentId)
	/**
	  * @param parentIds Targeted parent ids
	  * @return Copy of this access point that only includes text placements where parent id is within the
	  *  specified value set
	  */
	def withinTexts(parentIds: IterableOnce[Int]) = filter(placementModel.parentId.column.in(IntSet.from(parentIds)))
}
