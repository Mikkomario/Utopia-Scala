package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.event.KeyLocation
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.keyboard.Key
import utopia.genesis.handling.event.mouse.MouseButtonStateListener2.MouseButtonFilteringFactory
import utopia.genesis.handling.event.mouse.MouseMoveListener2.MouseMoveFilteringFactory
import utopia.genesis.handling.template.Handleable2
import utopia.paradigm.angular.{Angle, Rotation}

import scala.annotation.unused

object MouseDragListener2
{
	// TYPES    --------------------------
	
	/**
	  * A filter applied over mouse drag events
	  */
	type MouseDragEventFilter = Filter[MouseDragEvent2]
	
	
	// ATTRIBUTES   ----------------------
	
	/**
	  * A listener factory that doesn't apply any conditions or filters
	  */
	val unconditional = MouseDragListenerFactory()
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Access point to mouse drag event filters
	  */
	def filter = MouseDragEventFilter
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: MouseDragListener2.type): MouseDragListenerFactory = unconditional
	
	
	// NESTED   --------------------------
	
	trait MouseDragFilteringFactory[+A]
		extends MouseMoveFilteringFactory[MouseDragEvent2, A] with MouseButtonFilteringFactory[MouseDragEvent2, A]
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
		
		override protected def withFilter(filter: Filter[MouseDragEvent2]): MouseDragEventFilter = filter
		
		
		// OTHER    ------------------------
		
		/**
		  * @param f A filter function
		  * @return A filter that uses the specified function
		  */
		def apply(f: MouseDragEvent2 => Boolean): MouseDragEventFilter = Filter(f)
	}
	
	case class MouseDragListenerFactory(condition: FlagLike = AlwaysTrue, filter: MouseDragEventFilter = AcceptAll)
		extends ListenerFactory[MouseDragEvent2, MouseDragListenerFactory]
			with MouseDragFilteringFactory[MouseDragListenerFactory]
	{
		// IMPLEMENTED  -----------------------
		
		override def usingFilter(filter: Filter[MouseDragEvent2]): MouseDragListenerFactory = copy(filter = filter)
		override def usingCondition(condition: Changing[Boolean]): MouseDragListenerFactory =
			copy(condition = condition)
		
		override protected def withFilter(filter: Filter[MouseDragEvent2]): MouseDragListenerFactory =
			copy(filter = this.filter && filter)
			
		
		// OTHER    ---------------------------
		
		/**
		  * @param f A function to call on mouse drag events
		  * @tparam U Arbitrary function result type
		  * @return A listener that calls the specified function on mouse drag events,
		  *         but only when this factory's listening condition and event filter allow it
		  */
		def apply[U](f: MouseDragEvent2 => U): MouseDragListener2 = new _MouseDragListener[U](condition, filter, f)
	}
	
	private class _MouseDragListener[U](override val handleCondition: FlagLike,
	                                    override val mouseDragEventFilter: MouseDragEventFilter,
	                                    f: MouseDragEvent2 => U)
		extends MouseDragListener2
	{
		override def onMouseDrag(event: MouseDragEvent2): Unit = f(event)
	}
}

/**
 * Common trait for instances that are interested in mouse drag events
 * @author Mikko Hilpinen
 * @since 20.2.2023, v3.2.1
 */
trait MouseDragListener2 extends Handleable2
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return A filter for the accepted mouse drag events
	  */
	def mouseDragEventFilter: Filter[MouseDragEvent2]
	
	/**
	 * Allows the listener to react to mouse drag events
	 * @param event A new mouse drag event
	 */
	def onMouseDrag(event: MouseDragEvent2): Unit
}
