package utopia.logos.database.access.text.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on text placement properties
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait FilterTextPlacements[+Repr] extends FilterableView[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Model that defines text placement database properties
	  */
	def textPlacementModel: TextPlacementDbProps
	
	
	// OTHER	--------------------
	
	/**
	  * @param orderIndex order index to target
	  * @return Copy of this access point that only includes text placements with the specified order index
	  */
	def at(orderIndex: Int) = filter(textPlacementModel.orderIndex.column <=> orderIndex)
	
	/**
	  * @param orderIndices Targeted order indices
	  * @return Copy of this access point that only includes text placements where order index is within the 
	  * specified value set
	  */
	def atIndices(orderIndices: IterableOnce[Int]) = 
		filter(textPlacementModel.orderIndex.column.in(IntSet.from(orderIndices)))
	
	/**
	  * @param placedId placed id to target
	  * @return Copy of this access point that only includes text placements with the specified placed id
	  */
	def placing(placedId: Int) = filter(textPlacementModel.placedId.column <=> placedId)
	
	/**
	  * @param placedIds Targeted placed ids
	  * @return Copy of this access point that only includes text placements where placed id is within the 
	  * specified value set
	  */
	def placingTexts(placedIds: IterableOnce[Int]) = 
		filter(textPlacementModel.placedId.column.in(IntSet.from(placedIds)))
	
	/**
	  * @param parentId parent id to target
	  * @return Copy of this access point that only includes text placements with the specified parent id
	  */
	def withinText(parentId: Int) = filter(textPlacementModel.parentId.column <=> parentId)
	
	/**
	  * @param parentIds Targeted parent ids
	  * @return Copy of this access point that only includes text placements where parent id is within the 
	  * specified value set
	  */
	def withinTexts(parentIds: IterableOnce[Int]) = 
		filter(textPlacementModel.parentId.column.in(IntSet.from(parentIds)))
}

