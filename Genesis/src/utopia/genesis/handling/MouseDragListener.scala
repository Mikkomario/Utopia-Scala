package utopia.genesis.handling

import utopia.genesis.event.MouseDragEvent
import utopia.inception.handling.{Handleable, HandlerType}
import utopia.inception.util.{AnyFilter, Filter}

import scala.language.implicitConversions

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
object MouseDragListener
{
	// IMPLICIT ----------------------
	
	/**
	 * Converts a function into a mouse drag listener
	 * @param f A function that handles mouse drag events
	 * @return A new mouse drag listener
	 */
	implicit def apply(f: MouseDragEvent => Unit): MouseDragListener = new _MouseDragListener(f)
	
	
	// OTHER    ----------------------
	
	/**
	 * @param filter A filter to apply
	 * @param f A function for handling mouse drag events
	 * @return A new mouse drag listener
	 */
	def filtered(filter: Filter[MouseDragEvent])(f: MouseDragEvent => Unit): MouseDragListener =
		new _MouseDragListener(f, filter)
	
	
	// NESTED   ----------------------
	
	private class _MouseDragListener(f: MouseDragEvent => Unit, filter: Filter[MouseDragEvent] = AnyFilter)
		extends MouseDragListener
	{
		override def mouseDragFilter = filter
		
		override def onMouseDrag(event: MouseDragEvent): Unit = f(event)
		
		override def allowsHandlingFrom(handlerType: HandlerType): Boolean = true
	}
}

/**
 * Common trait for instances that are interested in mouse drag events
 * @author Mikko Hilpinen
 * @since 20.2.2023, v3.2.1
 */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
trait MouseDragListener extends Handleable
{
	// ABSTRACT ----------------------------
	
	/**
	 * Allows the listener to react to mouse drag events
	 * @param event A new mouse drag event
	 */
	def onMouseDrag(event: MouseDragEvent): Unit
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return A filter for the accepted mouse drag events
	 */
	def mouseDragFilter: Filter[MouseDragEvent] = AnyFilter
}
