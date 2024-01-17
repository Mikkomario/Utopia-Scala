package utopia.paradigm.shape.shape2d.insets

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.combine.Subtractable
import utopia.flow.util.NotEmpty
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.template.HasDimensions
import utopia.paradigm.transform.LinearSizeAdjustable

/**
  * Common trait for instances which specify length values for 0-4 sides
  * and support scaling and combining of length values.
  * @author Mikko Hilpinen
  * @since 16/01/2024, v1.5
  * @tparam L Type of lengths applied
  * @tparam L2D Type of 2-dimensional combination of the lengths applied
  * @tparam Repr Implementing type
  */
trait ScalableSidesLike[L, +L2D, +Repr]
	extends JoinableSidesLike[L, L2D, Repr] with LinearSizeAdjustable[Repr] with Subtractable[HasSides[L], Repr]
{
	// ABSTRACT -----------------------
	
	/**
	  * @param length Original length
	  * @param mod Scaling modifier
	  * @return Scaled length
	  */
	protected def multiply(length: L, mod: Double): L
	/**
	  * @param from Item to subtract from
	  * @param amount The amount of subtract
	  * @return Subtraction result
	  */
	protected def subtract(from: L, amount: L): L
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return The average width of these insets
	  */
	def average = NotEmpty(sides) match {
		case Some(sides) => multiply(sides.valuesIterator.reduce(join), 0.25)
		case None => zeroLength
	}
	
	
	// IMPLEMENTED  -------------------
	
	override def *(mod: Double): Repr = mapDefined { multiply(_, mod) }
	
	override def -(other: HasSides[L]): Repr = withSides(sides.mergeWith(other.sides)(subtract))
		
	
	// OTHER    ----------------------
	
	/**
	  * @param mod Scaling modifiers to apply (one for each side)
	  * @return A scaled copy of this item
	  */
	def *(mod: HasSides[Double]) = NotEmpty(sides) match {
		case Some(sides) =>
			NotEmpty(mod.sides) match {
				case Some(mod) =>
					withSides((sides.keySet & mod.keySet).map { side => side -> multiply(sides(side), mod(side)) }.toMap)
				case None => zero
			}
		case None => self
	}
	/**
	  * @param mod Scaling modifiers to apply (one for each axis)
	  * @return A scaled copy of this item
	  */
	def *(mod: HasDimensions[Double]) = mapWithDirection { (dir, length) => multiply(length, mod(dir.axis)) }
	
	/**
	  * @param len Amount of decrease to apply on for all directions
	  * @return Copy of this item with decreased sides
	  */
	def decreaseAllBy(len: L) = map { subtract(_, len) }
	/**
	  * @param len Targeted length and direction
	  * @return Copy of this item with the targeted direction decreased by the specified amount
	  */
	def -(len: (L, Direction2D)) = mapSide(len._2) { subtract(_, len._1) }
}
