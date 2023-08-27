package utopia.firmament.controller

import utopia.firmament.component.{AreaOfItems, Component}
import utopia.firmament.component.container.many.StackLike
import utopia.firmament.component.stack.Stackable
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.View
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.{Bounds, HasBounds}

object StackItemAreas
{
	/**
	  * Creates a new item area tracker that uses a custom components view
	  * @param stack Stack to track
	  * @param componentsView A view into the components in the stack
	  * @tparam C Type of the components
	  * @return A new item area tracker
	  */
	def apply[C <: HasBounds](stack: StackLike[_], componentsView: View[Seq[C]]) =
		new StackItemAreas[C](stack, componentsView)
	
	/**
	  * Creates a new stack item area tracker
	  * @param stack Stack to track
	  * @tparam C Type of the components inside the stack
	  * @return A new item area tracker
	  */
	def apply[C <: Component with Stackable](stack: StackLike[C]): StackItemAreas[C] =
		apply[C](stack, View { stack.components })
}

/**
  * A utility object used for tracking the locations of the items within a stack
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  */
class StackItemAreas[C <: HasBounds](stack: StackLike[_], componentsView: View[Seq[C]]) extends AreaOfItems[C]
{
	// IMPLEMENTED  ------------------------
	
	override def areaOf(item: C): Option[Bounds] = {
		// Caches components so that indexes won't change in between
		val c = componentsView.value
		val direction = stack.direction
		c.optionIndexOf(item).map { i =>
			if (c.size == 1)
				Bounds(Point.origin, stack.size)
			else {
				// Includes half of the area between items (if there is no item, uses cap)
				val top = if (i > 0) (item.position(direction) - c(i - 1).maxAlong(direction)) / 2 else
					item.position(direction)
				val bottom = if (i < c.size - 1) (c(i + 1).position(direction) - item.maxAlong(direction)) / 2 else
					stack.length - item.maxAlong(direction)
				
				// Also includes the whole stack breadth
				Bounds(item.position - direction(top), item.size.withDimension(direction.perpendicular(stack.breadth)) +
					direction(top + bottom))
			}
		}
	}
	
	override def itemNearestTo(relativePoint: Point): Option[C] = {
		val direction = stack.direction
		val p = relativePoint(direction)
		val c = componentsView.value
		// Finds the first item past the relative point
		c.indexWhereOption { _.position(direction) > p }.map { nextIndex =>
			// Selects the next item if a) it's the first item or b) it's closer to point than the previous item
			if (nextIndex == 0 || c(nextIndex).position(direction) - p < p - c(nextIndex - 1).maxAlong(direction))
				c(nextIndex)
			else
				c(nextIndex - 1)
			
		}.orElse(c.lastOption)
	}
}
