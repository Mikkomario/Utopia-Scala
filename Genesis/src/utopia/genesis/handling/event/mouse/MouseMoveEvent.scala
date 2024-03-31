package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.filter.Filter
import utopia.genesis.handling.event.mouse.MouseEvent.MouseFilteringFactory
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.OriginType.Relative
import utopia.paradigm.enumeration.{Direction2D, OriginType}
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import scala.concurrent.duration.FiniteDuration

object MouseMoveEvent
{
	// TYPES    ----------------------
	
	/**
	  * A filter that processes any type of mouse move events
	  */
	type MouseMoveEventFilter = Filter[MouseMoveEventLike[_]]
	
	
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
	
	
	// NESTED   ------------------------------
	
	trait MouseMoveFilteringFactory[+E <: MouseMoveEventLike[_], +A] extends MouseFilteringFactory[E, A]
	{
		// COMPUTED ------------------
		
		/**
		  * @return An item that only accepts mouse movements going (mostly) up
		  */
		def up = direction(Up)
		/**
		  * @return An item that only accepts mouse movements going (mostly) down
		  */
		def down = direction(Down)
		/**
		  * @return An item that only accepts mouse movements going (mostly) left
		  */
		def left = direction(Direction2D.Left)
		/**
		  * @return An item that only accepts mouse movements going (mostly) right
		  */
		def right = direction(Direction2D.Right)
		
		
		// OTHER    ------------------
		
		/**
		  * @param threshold A velocity threshold
		  * @return An item that only accepts movements with greater (or equal) velocity than the one specified
		  */
		def velocityOver(threshold: LinearVelocity) = withFilter { _.velocity.linear >= threshold }
		/**
		  * @param center Center of the targeted range of directions / angles
		  * @param maximumVariance Maximum difference (in either direction) from the central angle,
		  *                        that's still accepted
		  * @return An item that only accepts movements towards the specified direction
		  *         (with a certain amount of variance allowed)
		  */
		def directionWithin(center: Angle, maximumVariance: Rotation) = {
			val minimum = center + maximumVariance.counterclockwise
			val maximum = center + maximumVariance.clockwise
			if (minimum > maximum)
				withFilter { e =>
					val angle = e.transition.direction
					angle >= minimum || angle <= maximum
				}
			else
				withFilter { e =>
					val angle = e.transition.direction
					angle >= minimum && angle <= maximum
				}
		}
		/**
		  * @param direction Targeted direction
		  * @param maximumVariance How much the actual / precise direction angle may vary from the specified target
		  *                        (to either direction)
		  * @return An item that only accepts movement events towards the specified direction
		  *         (with the specified variance allowed)
		  */
		def direction(direction: Direction2D, maximumVariance: Rotation = Rotation.circles(0.2)) =
			directionWithin(Angle(direction), maximumVariance)
		
		/**
		  * @param area A function that yields the targeted area
		  * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
		  *                 or whether it is an "absolute" area (on the screen)
		  * @return An item that only accepts events where the starting location is within the specified area
		  */
		def startedOver(area: => Area2D, areaType: OriginType = Relative) =
			withFilter { _.startedOver(area, areaType) }
		/**
		  * @param area A function that yields the targeted area
		  * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
		  *                 or whether it is an "absolute" area (on the screen)
		  * @return An item that only accepts events where the starting location is outside the specified area
		  */
		def startedOutside(area: => Area2D, areaType: OriginType = Relative) =
			withFilter { _.startedOutside(area, areaType) }
		/**
		  * @param area A function that yields the targeted area
		  * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
		  *                 or whether it is an "absolute" area (on the screen)
		  * @return An item that only accepts events where the cursor moved within the specified area
		  */
		def entered(area: => Area2D, areaType: OriginType = Relative) = withFilter { _.entered(area, areaType) }
		/**
		  * @param area A function that yields the targeted area
		  * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
		  *                 or whether it is an "absolute" area (on the screen)
		  * @return An item that only accepts events where the cursor exited the specified area
		  */
		def exited(area: => Area2D, areaType: OriginType = Relative) = withFilter { _.exited(area, areaType) }
		/**
		  * @param area A function that yields the targeted area
		  * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
		  *                 or whether it is an "absolute" area (on the screen)
		  * @return An item that only accepts events where the cursor either entered or exited the specified area
		  */
		def enteredOrExited(area: => Area2D, areaType: OriginType = Relative) =
			withFilter { _.enteredOrExited(area, areaType) }
	}
	
	object MouseMoveEventFilter extends MouseMoveFilteringFactory[MouseMoveEventLike[_], MouseMoveEventFilter]
	{
		// IMPLEMENTED  ---------------------
		
		override protected def withFilter(filter: Filter[MouseMoveEventLike[_]]): MouseMoveEventFilter = filter
		
		
		// OTHER    -------------------------
		
		/**
		  * @param f A filtering function for mouse move events
		  * @return A filter that utilizes that function
		  */
		def apply(f: MouseMoveEventLike[_] => Boolean): MouseMoveEventFilter = Filter(f)
	}
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