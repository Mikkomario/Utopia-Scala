package utopia.paradigm.shape.shape2d.insets

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.combine.Combinable
import utopia.paradigm.enumeration.{Axis2D, Direction2D}

/**
  * Common trait for instances which specify a length value for 0-4 sides,
  * and which support the sum operation
  * @author Mikko Hilpinen
  * @since 16/01/2024, v1.5
  * @tparam L Type of length values used
  * @tparam L2D Type of 2-dimensional length values (combinations)
  * @tparam Repr Implementing type
  */
trait JoinableSidesLike[L, +L2D, +Repr]
	extends SidesLike[L, Repr] with HasJoinableSides[L, L, L2D] with Combinable[HasSides[L], Repr]
{
	// IMPLEMENTED  ------------------------
	
	override def +(other: HasSides[L]): Repr = withSides(sides.mergeWith(other.sides)(join))
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param len Length increase to apply along all directions
	  * @return Copy of this item with all sides extended
	  */
	def extendAllBy(len: L) = map { join(_, len) }
	/**
	  * @param len Length and direction
	  * @return Copy of this item with the targeted direction extended by the specified amount
	  */
	def +(len: (L, Direction2D)) = extendTowardsBy(len._2, len._1)
	
	/**
	  * @param direction Targeted direction
	  * @param increment Amount of increase to apply towards that direction
	  * @return Copy of this item with the targeted direction extended by the specified amount
	  */
	def extendTowardsBy(direction: Direction2D, increment: L) = mapSide(direction) { join(_, increment) }
	/**
	  * @param axis Targeted axis
	  * @param increment Amount of increase to apply towards **both directions** along that axis
	  * @return Copy of this item with the targeted sides extended by the specified amount
	  */
	def extendAlongBy(axis: Axis2D, increment: L) = mapAxis(axis) { join(_, increment) }
}
