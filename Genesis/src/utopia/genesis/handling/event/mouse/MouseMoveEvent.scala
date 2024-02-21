package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.filter.Filter
import utopia.genesis.handling.event.mouse.MouseMoveListener.MouseMoveEventFilter
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import scala.concurrent.duration.FiniteDuration

object MouseMoveEvent
{
	// COMPUTED -----------------------
	
	/**
	  * @return Access point for constructing mouse move -event filters
	  */
	def filter = MouseMoveEventFilter
	
	
	// OTHER    ----------------------
	
	/**
	  * Creates an event filter that only accepts mouse events originating from the mouse entering
	  * the specified area
	  * @param getArea A function for calculating the target area. Will be called each time an event needs to be filtered
	  */
	@deprecated("Please use .filter.entered(Area2D) instead", "v4.0")
	def enterAreaFilter(getArea: => Area2D): Filter[MouseMoveEvent] = e => e.enteredArea(getArea)
	/**
	  * Creates an event filter that only accepts mouse events originating from the mouse exiting the
	  * specified area
	  * @param getArea A function for calculating the target area. Will be called each time an event needs to be filtered.
	  */
	@deprecated("Please use .filter.exited(Area2D) instead", "v4.0")
	def exitedAreaFilter(getArea: => Area2D): Filter[MouseMoveEvent] = e => e.exitedArea(getArea)
	/**
	  * @param area The followed area (call-by-name)
	  * @return A filter that only accepts events where the mouse entered or exited the specified area
	  */
	@deprecated("Please use .filter.enteredOrExited(Area2D) instead", "v4.0")
	def enteredOrExitedAreaFilter(area: => Area2D): Filter[MouseMoveEvent] = { e =>
		val a = area
		Pair(e.mousePosition, e.previousMousePosition).isAsymmetricBy(a.contains)
	}
	
	/**
	  * Creates an event filter that only accepts events where the mouse cursor moved with enough
	  * speed
	  */
	@deprecated("Please use .filter.velocityOver(LinearVelocity) instead", "v4.0")
	def minVelocityFilter(minVelocity: LinearVelocity): Filter[MouseMoveEvent] = { e => e.velocity.linear >= minVelocity }
}

/**
  * An event generated whenever the mouse (cursor) moves
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
case class MouseMoveEvent(positions: Pair[RelativePoint], duration: FiniteDuration, buttonStates: MouseButtonStates)
	extends MouseMoveEventLike[MouseMoveEvent]
{
	// ATTRIBUTES   --------------------------
	
	override lazy val transition = super.transition
	override lazy val velocity = super.velocity
	
	
	// IMPLEMENTED  --------------------------
	
	/**
	  * @param positions New mouse positions (start & end) to assign
	  * @return Copy of this event with the specified positions
	  */
	override def withPositions(positions: Pair[RelativePoint]) = copy(positions = positions)
}