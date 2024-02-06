package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import scala.concurrent.duration.FiniteDuration

// TODO: Add deprecated utility functions from MouseMoveEvent

/**
  * An event generated whenever the mouse (cursor) moves
  * @author Mikko Hilpinen
  * @since 06/02/2024, v3.6
  */
case class MouseMoveEvent2(positions: Pair[RelativePoint], duration: FiniteDuration, buttonStates: MouseButtonStates)
	extends MouseMoveEventLike[MouseMoveEvent2]
{
	// ATTRIBUTES   --------------------------
	
	override lazy val transition = super.transition
	override lazy val velocity = super.velocity
	
	
	// IMPLEMENTED  --------------------------
	
	@deprecated("This function only affects the new mouse position. Please use .withPositions(Pair) instead")
	override def withPosition(position: RelativePoint): MouseMoveEvent2 =
		copy(positions = positions.withSecond(position))
	/**
	  * @param f A mapping function for relative mouse coordinates
	  * @return Copy of this event with both movement start and movement end positions mapped
	  */
	override def mapPosition(f: RelativePoint => RelativePoint) = withPositions(positions.map(f))
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param positions New mouse positions (start & end) to assign
	  * @return Copy of this event with the specified positions
	  */
	def withPositions(positions: Pair[RelativePoint]) = copy(positions = positions)
}