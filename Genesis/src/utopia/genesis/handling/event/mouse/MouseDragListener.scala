package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.mouse.MouseDragEvent.{MouseDragEventFilter, MouseDragFilteringFactory}
import utopia.genesis.handling.template.Handleable

import scala.annotation.unused
import scala.language.implicitConversions

object MouseDragListener
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A listener factory that doesn't apply any conditions or filters
	  */
	val unconditional = MouseDragListenerFactory()
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: MouseDragListener.type): MouseDragListenerFactory = unconditional
	
	
	// NESTED   --------------------------
	
	case class MouseDragListenerFactory(condition: Flag = AlwaysTrue, filter: MouseDragEventFilter = AcceptAll)
		extends ListenerFactory[MouseDragEvent, MouseDragListenerFactory]
			with MouseDragFilteringFactory[MouseDragListenerFactory]
	{
		// IMPLEMENTED  -----------------------
		
		override def usingFilter(filter: Filter[MouseDragEvent]): MouseDragListenerFactory = copy(filter = filter)
		override def usingCondition(condition: Flag): MouseDragListenerFactory = copy(condition = condition)
		
		override protected def withFilter(filter: Filter[MouseDragEvent]): MouseDragListenerFactory =
			copy(filter = this.filter && filter)
			
		
		// OTHER    ---------------------------
		
		/**
		  * @param f A function to call on mouse drag events
		  * @tparam U Arbitrary function result type
		  * @return A listener that calls the specified function on mouse drag events,
		  *         but only when this factory's listening condition and event filter allow it
		  */
		def apply[U](f: MouseDragEvent => U): MouseDragListener = new _MouseDragListener[U](condition, filter, f)
	}
	
	private class _MouseDragListener[U](override val handleCondition: Flag,
	                                    override val mouseDragEventFilter: MouseDragEventFilter,
	                                    f: MouseDragEvent => U)
		extends MouseDragListener
	{
		override def onMouseDrag(event: MouseDragEvent): Unit = f(event)
	}
}

/**
 * Common trait for instances that are interested in mouse drag events
 * @author Mikko Hilpinen
 * @since 20.2.2023, v3.2.1
 */
trait MouseDragListener extends Handleable
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return A filter for the accepted mouse drag events
	  */
	def mouseDragEventFilter: Filter[MouseDragEvent]
	
	/**
	 * Allows the listener to react to mouse drag events
	 * @param event A new mouse drag event
	 */
	def onMouseDrag(event: MouseDragEvent): Unit
}
