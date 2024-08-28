package utopia.logos.model.factory.text

import utopia.logos.model.template.PlacedFactory

/**
  * Common trait for text placement-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementFactory[+A] extends PlacedFactory[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param orderIndex New order index to assign
	  * @return Copy of this item with the specified order index
	  */
	def withOrderIndex(orderIndex: Int): A
	/**
	  * @param parentId New parent id to assign
	  * @return Copy of this item with the specified parent id
	  */
	def withParentId(parentId: Int): A
	/**
	  * @param placedId New placed id to assign
	  * @return Copy of this item with the specified placed id
	  */
	def withPlacedId(placedId: Int): A
	
	
	// IMPLEMENTED  ------------------
	
	override def at(orderIndex: Int): A = withOrderIndex(orderIndex)
}

