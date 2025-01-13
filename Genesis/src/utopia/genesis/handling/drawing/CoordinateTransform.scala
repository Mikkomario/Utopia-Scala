package utopia.genesis.handling.drawing

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.consume.ConsumeChoice.Preserve
import utopia.genesis.handling.event.mouse._
import utopia.genesis.handling.template.Handlers
import utopia.paradigm.shape.template.vector.DoubleVectorLike

/**
  * Common trait for "views" into a drawn instance that somehow transform the coordinate system.
  * Used for bridging location information between the different coordinate systems.
  * @author Mikko Hilpinen
  * @since 11/02/2024, v4.0
  */
trait CoordinateTransform
{
	// ABSTRACT ------------------------
	
	/**
	  * Converts a point in the space relative to this item, to a space relative to the wrapped item.
	  * @param p A point to convert
	  * @tparam V Type of the specified point
	  * @return A matching point in the view space (i.e. the wrapped item's coordinate system)
	  */
	def toView[V <: DoubleVectorLike[V]](p: V): V
	/**
	  * Converts a point from the view space (i.e. from space relative to the wrapped item),
	  * to space relative to this item.
	  * @param viewPoint A point in the wrapped item's space / view space.
	  * @tparam V Type of the specified point
	  * @return A matching point in this item's space
	  */
	def view[V <: DoubleVectorLike[V]](viewPoint: V): V
	
	/**
	  * Starts delivering of transformed mouse events to the wrapped wrapped item and/or possibly other items.
	  * @param parentHandlers The handlers that will deliver mouse events to be converted.
	  *                       The supported (& expected) handler types are:
	  *                             - [[MouseButtonStateHandler]]
	  *                             - [[MouseMoveHandler]]
	  *                             - [[MouseWheelHandler]]
	  *                             - [[MouseDragHandler]]
	  * @param disableMouseToWrapped Whether direct mouse-event delivery to the wrapped item should be disabled,
	  *                              even in situations where it would otherwise be possible.
	  *                              Set this to true if you don't want the wrapped item to receive these mouse events
	  *                              (directly).
	  *                              Default = false = the wrapped item will start to receive mouse events,
	  *                              if its capable of handling them.
	  * @return Handlers that deliver transformed mouse events
	  */
	def setupMouseEvents(parentHandlers: Handlers, disableMouseToWrapped: Boolean = false): Handlers
	
	
	// NESTED   -----------------------
	
	/**
	  * A mouse handler class that uses this coordinate transformation to deliver transformed mouse events
	  */
	protected class TransformedMouseHandler(implicit log: Logger)
		extends MouseMoveListener with MouseWheelListener with MouseDragListener with MouseButtonStateListener
	{
		// ATTRIBUTES   ----------------------
		
		private val buttonHandler = MouseButtonStateHandler.empty
		private val moveHandler = MouseMoveHandler.empty
		private val wheelHandler = MouseWheelHandler.empty
		private val dragHandler = MouseDragHandler.empty
		
		val handlers = Handlers(buttonHandler, moveHandler, wheelHandler, dragHandler)
		
		override lazy val handleCondition: Flag = buttonHandler.handleCondition || moveHandler.handleCondition ||
			wheelHandler.handleCondition || dragHandler.handleCondition
		
		
		// IMPLEMENTED  ----------------------
		
		override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] = AcceptAll
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		override def mouseWheelEventFilter: Filter[MouseWheelEvent] = AcceptAll
		override def mouseDragEventFilter: Filter[MouseDragEvent] = AcceptAll
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = {
			if (buttonHandler.mayBeHandled)
				buttonHandler.onMouseButtonStateEvent(transform(event))
			else
				Preserve
		}
		override def onMouseMove(event: MouseMoveEvent): Unit = {
			if (moveHandler.mayBeHandled)
				moveHandler.onMouseMove(transform(event))
		}
		override def onMouseWheelRotated(event: MouseWheelEvent): ConsumeChoice = {
			if (wheelHandler.mayBeHandled)
				wheelHandler.onMouseWheelRotated(transform(event))
			else
				Preserve
		}
		override def onMouseDrag(event: MouseDragEvent): Unit = {
			if (dragHandler.mayBeHandled)
				dragHandler.onMouseDrag(transform(event))
		}
		
		
		// OTHER    ------------------------
		
		private def transform[E <: MouseEvent[E]](event: E) = event.mapPosition { _.mapRelative(toView) }
	}
}
