package utopia.genesis.handling.drawing

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.{DrawOrder, Drawer}
import utopia.genesis.handling.event.ConsumeChoice
import utopia.genesis.handling.event.ConsumeChoice.Preserve
import utopia.genesis.handling.event.mouse._
import utopia.genesis.handling.template.Handlers
import utopia.paradigm.enumeration.FillAreaLogic.{Fit, ScalePreservingShape}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.vector.DoubleVectorLike

/**
  * Modifies where and how large another Drawable item is drawn
  * @author Mikko Hilpinen
  * @since 11/02/2024, v4.0
  */
class Repositioner(wrapped: Drawable2, targetPointer: Either[(Changing[Point], Changing[Size]), Changing[Bounds]],
                   resizeLogic: ScalePreservingShape = Fit)
	extends Drawable2
{
	// ATTRIBUTES   --------------------
	
	private val (targetPositionPointer, targetSizePointer) =
		targetPointer.leftOrMap { b => b.strongMap { _.position } -> b.strongMap { _.size } }
	private val originalSizePointer = wrapped.drawBoundsPointer.strongMap { _.size }
	
	private val relativeBoundsPointer = originalSizePointer.mergeWith(targetSizePointer)(resizeLogic.apply)
	override val drawBoundsPointer = relativeBoundsPointer.mergeWith(targetPositionPointer) { _ + _ }
	
	private val scalingPointer = targetSizePointer.mergeWith(originalSizePointer) { _.x / _.x }
	
	private lazy val mouseHandler = new RepositionedMouseHandler()
	
	private var _repaintListeners = Vector.empty[RepaintListener]
	
	
	// INITIAL CODE --------------------

	// Whenever the wrapped item requests a repaint, modifies the repaint call to match the new position
	wrapped.addRepaintListener { (_, region, priority) => repaint(region.map { _ * scalingPointer.value }, priority) }
	
	
	// IMPLEMENTED  --------------------
	
	override def handleCondition: FlagLike = wrapped.handleCondition
	
	override def drawOrder: DrawOrder = wrapped.drawOrder
	override def opaque: Boolean = wrapped.opaque
	override protected def repaintListeners: Iterable[RepaintListener] = _repaintListeners
	
	override def addRepaintListener(listener: RepaintListener): Unit = {
		if (!_repaintListeners.contains(listener))
			_repaintListeners :+= listener
	}
	override def removeRepaintListener(listener: RepaintListener): Unit =
		_repaintListeners = _repaintListeners.filterNot { _ == listener }
	
	override def draw(drawer: Drawer, bounds: Bounds): Unit = {
		// Modifies the drawer so that (0,0) lies at the targeted draw position, with correct scaling applied
		val modifiedDrawer = drawer
			.translated(bounds.position + relativeBoundsPointer.value.position)
			.scaled(scalingPointer.value)
		wrapped.draw(modifiedDrawer, Bounds(Point.origin, wrapped.drawBounds.size))
	}
	
	
	// OTHER    -----------------------
	
	/**
	  * Converts a point in the space relative to this item, to a space relative to the wrapped item.
	  * @param p A point to convert
	  * @tparam V Type of the specified point
	  * @return A matching point in the view space (i.e. the wrapped item's coordinate system)
	  */
	def toView[V <: DoubleVectorLike[V]](p: V) = {
		// Converts to a point relative to the displayed item draw-bounds
		val relativeToDrawArea = p - targetPositionPointer.value - relativeBoundsPointer.value.position
		// Applies scaling to match relative position to the item draw-bounds
		val scaled = relativeToDrawArea / scalingPointer.value
		// Corrects for the view position
		scaled + wrapped.drawBounds.position
	}
	/**
	  * Converts a point from the view space (i.e. from space relative to the wrapped item),
	  * to space relative to this item.
	  * @param viewPoint A point in the wrapped item's space / view space.
	  * @tparam V Type of the specified point
	  * @return A matching point in this item's space
	  */
	def view[V <: DoubleVectorLike[V]](viewPoint: V) = {
		// Converts to a point relative to the wrapped item's draw bounds
		val relativeToItemDrawBounds = viewPoint - wrapped.drawBounds.position
		// Applies scaling to match the position in the visual space
		val scaled = relativeToItemDrawBounds * scalingPointer.value
		// Corrects for the display position
		scaled + targetPositionPointer.value
	}
	
	/**
	  * Starts delivering of transformed mouse events to the wrapped wrapped item and/or possibly other items.
	  * @param parentHandlers The handlers that will deliver mouse events to be converted.
	  *                       The supported (& expected) handler types are:
	  *                             - [[MouseButtonStateHandler]]
	  *                             - [[MouseMoveHandler]]
	  *                             - [[MouseWheelHandler2]]
	  *                             - [[MouseDragHandler2]]
	  * @param disableMouseToWrapped Whether direct mouse-event delivery to the wrapped item should be disabled,
	  *                              even in situations where it would otherwise be possible.
	  *                              Set this to true if you don't want the wrapped item to receive these mouse events
	  *                              (directly).
	  *                              Default = false = the wrapped item will start to receive mouse events,
	  *                              if its capable of handling them.
	  * @return Handlers that deliver transformed mouse events
	  */
	def setupMouseEvents(parentHandlers: Handlers, disableMouseToWrapped: Boolean = false) = {
		// If the wrapped item supports mouse events, starts delivering them to that item as well (unless disabled)
		if (!disableMouseToWrapped && (wrapped.isInstanceOf[MouseMoveListener2] ||
			wrapped.isInstanceOf[MouseButtonStateListener2] || wrapped.isInstanceOf[MouseWheelListener2] ||
			wrapped.isInstanceOf[MouseDragListener2]))
			mouseHandler.handlers += wrapped
		
		// Starts receiving events from above
		parentHandlers += mouseHandler
		// Returns the converted mouse handlers
		mouseHandler.handlers
	}
	
	
	// NESTED   -----------------------
	
	private class RepositionedMouseHandler
		extends MouseMoveListener2 with MouseWheelListener2 with MouseDragListener2 with MouseButtonStateListener2
	{
		// ATTRIBUTES   ----------------------
		
		private val buttonHandler = MouseButtonStateHandler.empty
		private val moveHandler = MouseMoveHandler.empty
		private val wheelHandler = MouseWheelHandler2.empty
		private val dragHandler = MouseDragHandler2.empty
		
		val handlers = Handlers(buttonHandler, moveHandler, wheelHandler, dragHandler)
		
		override lazy val handleCondition: FlagLike =
			buttonHandler.handleCondition || moveHandler.handleCondition ||
				wheelHandler.handleCondition || dragHandler.handleCondition
		
		
		// IMPLEMENTED  ----------------------
		
		override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent2] = AcceptAll
		override def mouseMoveEventFilter: Filter[MouseMoveEvent2] = AcceptAll
		override def mouseWheelEventFilter: Filter[MouseWheelEvent2] = AcceptAll
		override def mouseDragEventFilter: Filter[MouseDragEvent2] = AcceptAll
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent2): ConsumeChoice = {
			if (buttonHandler.mayBeHandled)
				buttonHandler.onMouseButtonStateEvent(event.mapPosition { _.map(toView) })
			else
				Preserve
		}
		override def onMouseMove(event: MouseMoveEvent2): Unit = {
			if (moveHandler.mayBeHandled)
				moveHandler.onMouseMove(event.mapPosition { _.map(toView) })
		}
		override def onMouseWheelRotated(event: MouseWheelEvent2): ConsumeChoice = {
			if (wheelHandler.mayBeHandled)
				wheelHandler.onMouseWheelRotated(event.mapPosition { _.map(toView) })
			else
				Preserve
		}
		override def onMouseDrag(event: MouseDragEvent2): Unit = {
			if (dragHandler.mayBeHandled)
				dragHandler.onMouseDrag(event.mapPosition { _.map(toView) })
		}
	}
}
