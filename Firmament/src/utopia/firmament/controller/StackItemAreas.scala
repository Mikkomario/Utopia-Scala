package utopia.firmament.controller

import utopia.firmament.component.AreaOfItems
import utopia.firmament.component.container.many.StackLike
import utopia.firmament.component.stack.Stackable
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.{HasInclusiveEnds, NumericSpan}
import utopia.flow.view.immutable.View
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.{Bounds, HasBounds}
import utopia.paradigm.shape.shape2d.vector.point.Point

object StackItemAreas
{
	/**
	 * Creates a new stack item area tracker
	 * @param stack Stack to track
	 * @tparam C Type of the components inside the stack
	 * @return A new item area tracker
	 */
	def apply[C <: Stackable with HasBounds](stack: StackLike[C]): StackItemAreas[C] =
		apply[C](stack, View { stack.components })
	/**
	  * Creates a new item area tracker that uses a custom components view
	  * @param stack Stack to track
	  * @param componentsView A view into the components in the stack
	  * @tparam C Type of the components
	  * @return A new item area tracker
	  */
	def apply[C <: HasBounds](stack: StackLike[_], componentsView: View[Seq[C]]) =
		new StackItemAreas[C](stack, componentsView)
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
		val components = componentsView.value
		components.findIndexOf(item).map { _areaAt(components, components.size, _) }
	}
	
	override def itemNearestTo(relativePoint: Point): Option[C] = {
		val direction = stack.direction
		val p = relativePoint(direction)
		val c = componentsView.value
		// Finds the first item past the relative point
		c.findIndexWhere { _.position(direction) > p }.map { nextIndex =>
			// Selects the next item if a) it's the first item or b) it's closer to point than the previous item
			if (nextIndex == 0 || c(nextIndex).position(direction) - p < p - c(nextIndex - 1).maxAlong(direction))
				c(nextIndex)
			else
				c(nextIndex - 1)
			
		}.orElse(c.lastOption)
	}
	
	
	// OTHER    -----------------------------
	
	/**
	 * Finds the (relative) area surrounding components at the specified index range
	 * @param indices Targeted index range
	 * @return Area surrounding the targeted components. None if the specified index range was completely out of range.
	 *         Note: The returned bounds are relative.
	 *         I.e. the (0,0) location is placed in the top-left corner of the stack.
	 */
	def areaAt(indices: HasInclusiveEnds[Int]): Option[Bounds] = {
		indices.only match {
			// Case: Only targeting a single index => Uses a simpler function
			case Some(onlyIndex) => areaAt(onlyIndex)
			// Case: Targeting multiple indices
			case None =>
				val ascendingIndices = indices.toNumericSpan.ascending
				lazy val components = componentsView.value
				lazy val componentCount = components.size
				// Case: Fully out of the valid range => Yields None
				if (ascendingIndices.end < 0 || ascendingIndices.start >= componentCount)
					None
				else {
					lazy val direction = stack.direction
					// Case: Starting from the first component in the stack
					//       => Determines the length of the captured area
					if (ascendingIndices.start <= 0) {
						// Case: Covering the whole stack => Yields the whole stack's size
						if (ascendingIndices.end >= componentCount - 1)
							Some(Bounds(Point.origin, stack.size))
						// Case: Covering a subregion => The end threshold is between components
						else {
							val (last, after) = Pair.iterate(ascendingIndices.end) { _ + 1 }
								.map(components.apply).toTuple
							val end = ((last.maxAlong(direction) + after.minAlong(direction)) / 2).ceil
							Some(Bounds(Point.origin, stack.size.withDimension(direction, end)))
						}
					}
					else {
						val myIndices = NumericSpan(0, componentCount - 1)
						// Covers the whole stack's breadth
						val breadthRange = NumericSpan(0, stack.lengthAlong(direction.perpendicular))
						// Determines the targeted length by identifying the thresholds between the edge components
						val lengthRange = {
							// Case: Ending with the last component => The end is at the stack's end
							if (ascendingIndices.end >= myIndices.end) {
								val (first, before) = Pair.iterate(ascendingIndices.start) { _ - 1 }
									.map(components.apply).toTuple
								val start = ((before.maxAlong(direction) + first.minAlong(direction)) / 2).floor
								NumericSpan(start, stack.lengthAlong(direction))
							}
							else {
								val (first, last) = ascendingIndices.ends.map(components.apply).toTuple
								val (before, after) = ascendingIndices.extendedBy(1, bothDirections = true).ends
									.map(components.apply).toTuple
								val start = ((before.maxAlong(direction) + first.minAlong(direction)) / 2).floor
								val end = ((last.maxAlong(direction) + after.minAlong(direction)) / 2).ceil
								NumericSpan(start, end)
							}
						}
						Some(Bounds(lengthRange, breadthRange, direction))
					}
				}
		}
	}
	/**
	 * Identifies the stack's area surrounding a component at a specific position / index
	 * @param index Targeted component index
	 * @return Bounds around the targeted component. None if the index was out of range.
	 *         Note: The returned bounds are relative.
	 *         I.e. the (0,0) location is placed in the top-left corner of the stack.
	 */
	def areaAt(index: Int) = {
		// Case: Out of range
		if (index < 0)
			None
		else {
			val components = componentsView.value
			val componentCount = components.size
			// Case: Out of range
			if (index >= componentCount)
				None
			// Case: In range => Uses a delegate function
			else
				Some(_areaAt(components, componentCount, index))
		}
	}
	
	/**
	 * Calculates the area at a specific index
	 * @param components Components within the stack
	 * @param componentCount Number of components
	 * @param index Targeted index. MUST be within the valid component range.
	 * @return Bounds around the component at that index
	 */
	private def _areaAt(components: Seq[C], componentCount: Int, index: Int) = {
		// Case: This stack only contains a single component => Yields the whole stack's area
		if (componentCount == 1)
			Bounds(Point.origin, stack.size)
		// Case: Targeting a subregion
		else {
			val direction = stack.direction
			val primary = components(index)
			
			// Case: Targeting the first component => Finds the threshold between the first and the second component
			if (index == 0) {
				val end = ((primary.maxAlong(direction) + components(1).minAlong(direction)) / 2).ceil
				Bounds(Point.origin, stack.size.withHeight(end))
			}
			else {
				// The breadth is the whole stack's area
				val breadthRange = NumericSpan(0, stack.lengthAlong(direction.perpendicular))
				// The length thresholds are between the primary and the surrounding components
				val lengthRange = {
					// Case: Targeting the last component => The end threshold is the end of the stack
					if (index == componentCount - 1) {
						val start = ((components(index - 1).maxAlong(direction) + primary.minAlong(direction)) / 2)
							.floor
						NumericSpan(start, stack.lengthAlong(direction))
					}
					else {
						val (before, after) = Pair(-1, 1).view.map { adjust => components(index + adjust) }.toTuple
						val start = ((before.maxAlong(direction) + primary.minAlong(direction)) / 2).floor
						val end = ((primary.maxAlong(direction) + after.minAlong(direction)) / 2).ceil
						NumericSpan(start, end)
					}
				}
				Bounds(lengthRange, breadthRange, direction)
			}
		}
	}
}
