package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.filter.Filter
import utopia.genesis.handling.event.keyboard.{Key, KeyLocation, KeyboardState}
import utopia.genesis.handling.event.mouse.MouseButtonStateEvent.MouseButtonFilteringFactory
import utopia.genesis.handling.event.mouse.MouseMoveEvent.MouseMoveFilteringFactory
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import scala.concurrent.duration.FiniteDuration

object MouseDragEvent
{
	// TYPES    --------------------------
	
	/**
	  * A filter applied over mouse drag events
	  */
	type MouseDragEventFilter = Filter[MouseDragEvent]
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Access points to mouse drag -event filters
	  */
	def filter = MouseDragEventFilter
	
	
	// NESTED   --------------------------
	
	trait MouseDragFilteringFactory[+A]
		extends MouseMoveFilteringFactory[MouseDragEvent, A] with MouseButtonFilteringFactory[MouseDragEvent, A]
	{
		// COMPUTED ----------------------
		
		/**
		  * @return An item that only accepts drag started -events
		  */
		def started = withFilter { _.isDragStart }
		/**
		  * @return An item that only accepts drag ended -events
		  */
		def ended = withFilter { _.isDragEnd }
		/**
		  * @return An item that accepts drag started and drag ended -events
		  */
		def ends = withFilter { e => e.isDragEnd || e.isDragStart }
		
		/**
		  * @return An item that only accepts drags made with the left mouse button
		  */
		def withLeftButton = super[MouseButtonFilteringFactory].left
		/**
		  * @return An item that only accepts drags made with the right mouse button
		  */
		def withRightButton = super[MouseButtonFilteringFactory].right
		
		
		// IMPLEMENTED  ------------------
		
		override def left = super[MouseMoveFilteringFactory].left
		override def right = super[MouseMoveFilteringFactory].right
		
		// WET WET (from MouseMoveListener)
		override def directionWithin(center: Angle, maximumVariance: Rotation) = {
			val minimum = center + maximumVariance.counterclockwise
			val maximum = center + maximumVariance.clockwise
			if (minimum > maximum)
				withFilter { e =>
					val angle = e.totalMovement.direction
					angle >= minimum || angle <= maximum
				}
			else
				withFilter { e =>
					val angle = e.totalMovement.direction
					angle >= minimum && angle <= maximum
				}
		}
		
		
		// OTHER    ----------------------
		
		/**
		  * @param minimumDistance Minimum total drag distance
		  * @return An item that only accepts drag events after the linear drag distance grows long enough
		  */
		def furtherThan(minimumDistance: Double) = withFilter { _.totalMovement.length >= minimumDistance }
		
		/**
		  * @param key A keyboard key
		  * @return An item that only accepts drag events where the specified key was pressed at drag start
		  */
		def startedWithKeyPressed(key: Key) = withFilter { _.startKeyboardState(key) }
		/**
		  * @param key A keyboard key
		  * @return An item that only accepts drag events where the specified key was pressed at drag end
		  */
		def startedWithKeyPressed(key: Key, location: KeyLocation) =
			withFilter { _.startKeyboardState(key, location) }
	}
	
	object MouseDragEventFilter extends MouseDragFilteringFactory[MouseDragEventFilter]
	{
		// IMPLEMENTED  ---------------------
		
		override protected def withFilter(filter: Filter[MouseDragEvent]): MouseDragEventFilter = filter
		
		
		// OTHER    ------------------------
		
		/**
		  * @param f A filter function
		  * @return A filter that uses the specified function
		  */
		def apply(f: MouseDragEvent => Boolean): MouseDragEventFilter = Filter(f)
	}
}

/**
 * An event fired when the mouse is moved from one location to another, while holding a mouse button down
 * @author Mikko Hilpinen
 * @since 20.2.2023, v3.2
 * @param dragOrigin The position where the drag started
 * @param lastMove The latest mouse movement event
 * @param button The mouse button used in this drag
 * @param startKeyboardState Keyboard state at drag start
 * @param pressed Whether the associated mouse button is still pressed.
  *                False if the button was released, which also signals the end of the drag.
 */
case class MouseDragEvent(dragOrigin: RelativePoint, lastMove: MouseMoveEvent, override val button: MouseButton,
                          startKeyboardState: KeyboardState, override val pressed: Boolean = true)
	extends MouseMoveEventLike[MouseDragEvent] with MouseButtonStateEventLike[MouseDragEvent]
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * @return The total (linear) movement occurred during this drag (i.e. not just during this event)
	  */
	lazy val totalMovement = position.toVector - dragOrigin
	
	
	// COMPUTED --------------------------------
	
	/**
	 * @return Whether this event represents the end of a drag
	 */
	def isDragEnd = !pressed
	/**
	 * @return Whether this event represents the start of a drag
	 */
	def isDragStart = pressed && dragOrigin.relative == lastMove.previousPosition.relative
	
	@deprecated("Please use .startKeyboardState instead", "v4.0")
	def startKeyStatus = startKeyboardState
	
	
	// IMPLEMENTED  ----------------------------
	
	override def positions: Pair[RelativePoint] = lastMove.positions
	override def duration: FiniteDuration = lastMove.duration
	override def buttonStates: MouseButtonStates = lastMove.buttonStates
	
	override def withPositions(positions: Pair[RelativePoint]): MouseDragEvent =
		copy(lastMove = lastMove.withPositions(positions))
	override def mapPosition(f: RelativePoint => RelativePoint) =
		copy(dragOrigin = f(dragOrigin), lastMove = lastMove.mapPosition(f))
}
