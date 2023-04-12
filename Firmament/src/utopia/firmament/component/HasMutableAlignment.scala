package utopia.firmament.component

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.LinearAlignment.Middle
import utopia.paradigm.enumeration.{Alignment, Axis2D, Direction2D, LinearAlignment}

/**
  * Common trait for items that have an alignment state, which may be altered
  * @author Mikko Hilpinen
  * @since 11.4.2023, v1.0
  */
trait HasMutableAlignment
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The current alignment of this item
	  */
	def alignment: Alignment
	def alignment_=(newAlignment: Alignment): Unit
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return The current horizontal alignment of this item
	  */
	def horizontalAlignment = alignmentAlong(X)
	def horizontalAlignment_=(newAlignment: LinearAlignment) = mapAlignment { _.withHorizontal(newAlignment) }
	
	/**
	  * @return The current vertical alignment of this item
	  */
	def verticalAlignment = alignmentAlong(Y)
	def verticalAlignment_=(newAlignment: LinearAlignment) = mapAlignment { _.withVertical(newAlignment) }
	
	
	// OTHER    ----------------------
	
	/**
	  * Updates this item's alignment using a mapping function
	  * @param f A function that modifies this item's alignment
	  */
	def mapAlignment(f: Alignment => Alignment) = alignment = f(alignment)
	
	/**
	  * @param axis Targeted axis
	  * @return This item's alignment along that axis
	  */
	def alignmentAlong(axis: Axis2D) = alignment(axis)
	/**
	  * Updates this item's alignment along a specific axis
	  * @param axis Targeted axis
	  * @param alignment New alignment to apply on that axis
	  */
	def alignAlong(axis: Axis2D, alignment: LinearAlignment) = mapAlignment { _.withDimension(axis, alignment) }
	
	/**
	  * Centers this item along the specified axis
	  * @param axis Targeted axis
	  */
	def centerAlong(axis: Axis2D) = alignAlong(axis, Middle)
	/**
	  * Centers this item horizontally
	  */
	def centerHorizontally() = centerAlong(X)
	/**
	  * Centers this item vertically
	  */
	def centerVertically() = centerAlong(Y)
	
	/**
	  * Aligns this item towards the specified direction
	  * @param direction A direction to which this item should be aligned
	  */
	def alignTo(direction: Direction2D) = mapAlignment { _.toDirection(direction) }
}
