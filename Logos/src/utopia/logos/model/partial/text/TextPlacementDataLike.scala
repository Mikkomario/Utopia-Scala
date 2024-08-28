package utopia.logos.model.partial.text

import utopia.logos.model.factory.text.TextPlacementFactory

/**
  * Common trait for classes which provide read and copy access to text placement properties
  * @tparam Repr Implementing data class or data wrapper class
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementDataLike[+Repr] extends HasTextPlacementProps with TextPlacementFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Builds a modified copy of this text placement
	  * @param parentId New parent id to assign. Default = current value.
	  * @param placedId New placed id to assign. Default = current value.
	  * @param orderIndex New order index to assign. Default = current value.
	  * @return A copy of this text placement with the specified properties
	  */
	def copyTextPlacement(parentId: Int = parentId, placedId: Int = placedId, 
		orderIndex: Int = orderIndex): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def withOrderIndex(orderIndex: Int) = copyTextPlacement(orderIndex = orderIndex)
	override def withParentId(parentId: Int) = copyTextPlacement(parentId = parentId)
	override def withPlacedId(placedId: Int) = copyTextPlacement(placedId = placedId)
}

