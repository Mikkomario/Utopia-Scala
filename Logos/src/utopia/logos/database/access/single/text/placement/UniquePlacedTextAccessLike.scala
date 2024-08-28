package utopia.logos.database.access.single.text.placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.logos.model.template.PlacedFactory
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.UniqueModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which target individual texts linked to their specific individual placements
  * @tparam A Type of pulled texts
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 28.08.2024, v0.3
  */
trait UniquePlacedTextAccessLike[+A, +Repr]
	extends UniqueModelAccess[A] with FilterableView[Repr] with PlacedFactory[Repr] with Indexed
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Access to interacted text placement properties
	  */
	protected def placementModel: TextPlacementDbProps
	
	
	// COMPUTED	--------------------
	
	/**
	  * Id of the text within which this text appears. None if no text was accessible.
	  * @param connection Implicit DB connection
	  */
	def parentId(implicit connection: Connection) = pullColumn(placementModel.parentId).int
	/**
	  * Position index of the accessible text within the parent text. None if no text was accessible.
	  * @param connection Implicit DB connection
	  */
	def orderIndex(implicit connection: Connection) = pullColumn(placementModel.orderIndex).int
	
	
	// IMPLEMENTED  ----------------------
	
	override def at(orderIndex: Int): Repr = filter(placementModel.orderIndex <=> orderIndex)
	
	
	// OTHER	--------------------
	
	/**
	  * @param parentId Id of the targeted parent text
	  * @return Conditional copy of this access point,
	  * which only returns the item if it is linked to the specific text
	  */
	def withinText(parentId: Int) = filter(placementModel.parentId <=> parentId)
}
