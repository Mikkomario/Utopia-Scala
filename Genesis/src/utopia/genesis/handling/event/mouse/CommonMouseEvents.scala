package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.ConsumeChoice
import utopia.genesis.handling.template.{Handleable2, Handlers}

import scala.collection.mutable

/**
  * Used for distributing mouse events inside the application, regardless of the active window.
  * This represents kind of the root mouse event source.
  * @author Mikko Hilpinen
  * @since 12.9.2020, v2.4
  */
object CommonMouseEvents extends mutable.Growable[Handleable2]
{
	// ATTRIBUTES	--------------------------------
	
	private var generators = Set[MouseEventGenerator2]()
	private var _buttonStates = MouseButtonStates.default
	
	private val buttonHandler = MouseButtonStateHandler.empty
	private val moveHandler = MouseMoveHandler.empty
	private val wheelHandler = MouseWheelHandler2.empty
	private val dragHandler = MouseDragHandler2.empty
	
	/**
	  * The handlers (button, move, wheel & drag) managed by this interface
	  */
	val handlers = Handlers(buttonHandler, moveHandler, wheelHandler, dragHandler)
	
	private val dragTracker = new DragTracker2(AbsolutizingListener)
	
	
	// INITIAL CODE --------------------------------
	
	buttonHandler += dragTracker
	moveHandler += dragTracker
	
	
	// COMPUTED -----------------------------------
	
	/**
	  * @return Current mouse button states
	  */
	def buttonStates = _buttonStates
	
	
	// IMPLEMENTED  -------------------------------
	
	override def addOne(elem: Handleable2) = {
		handlers += elem
		this
	}
	
	override def clear() = handlers.clear()
	
	
	// OTHER	------------------------------------
	
	/**
	  * Registers a new event generator to be used for generating global mouse events
	  * @param generator A new mouse event generator
	  */
	def addGenerator(generator: MouseEventGenerator2) = {
		if (!generators.contains(generator) && generator.hasNotStopped) {
			generators = generators + generator
			generator.handlers += AbsolutizingListener
			
			generator.stoppedFlag.onceSet { removeGenerator(generator) }
		}
	}
	@deprecated("Please use .addGenerator(MouseEventGenerator2) instead", "v4.0")
	def registerGenerator(generator: MouseEventGenerator2) = addGenerator(generator)
	/**
	  * Removes a mouse event generator from the tracked / listened generators
	  * @param generator A generator to remove
	  */
	def removeGenerator(generator: MouseEventGenerator2) = {
		if (generators.contains(generator)) {
			generator.handlers -= AbsolutizingListener
			generators -= generator
		}
	}
	@deprecated("Please use .removeGenerator(MouseEventGenerator) instead", "v4.0")
	def unregisterGenerator(generator: MouseEventGenerator2) = removeGenerator(generator)
	
	/**
	  * Registers a new mouse button listener to this handler
	  * @param listener A new mouse button listener
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def registerButtonListener(listener: MouseButtonStateListener2) =
		buttonHandler += listener
	/**
	  * Registers a new mouse move listener to this handler
	  * @param listener A mouse move listener
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def registerMoveListener(listener: MouseMoveListener2) =
		moveHandler += listener
	/**
	  * Registers a new mouse wheel listener to this handler
	  * @param listener A mouse wheel listener
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def registerWheelListener(listener: MouseWheelListener2) =
		wheelHandler += listener
	/**
	 * @param listener A new mouse drag listener to inform about mouse drag events
	 */
	@deprecated("Deprecated for removal", "v4.0")
	def registerDragListener(listener: MouseDragListener2) =
		dragHandler += listener
	
	/**
	  * Registers a mouse event generator or a mouse related listener. If the specified instance is neither of these,
	  * does nothing
	  * @param item A generator or listener to register
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def register(item: Any) = item match {
		case generator: MouseEventGenerator2 => registerGenerator(generator)
		case listener: Handleable2 => handlers += listener
		case _ => ()
	}
	/**
	  * Removes a mouse event generator of a mouse related listener from this system
	  * @param item A generator or listener to remove
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def unregister(item: Any) = item match {
		case generator: MouseEventGenerator2 => unregisterGenerator(generator)
		case listener: Handleable2 => handlers -= listener
		case _ => ()
	}
	
	/**
	  * Removes a mouse event listener
	  * @param listener A listener that will no longer receive mouse-related events
	  */
	def -=(listener: Handleable2) = handlers -= listener
	/**
	  * Removes 0-n mouse event listeners
	  * @param listeners Listeners to remove from receiving mouse-related events
	  */
	def --=(listeners: IterableOnce[Handleable2]) = handlers --= listeners
	
	
	// NESTED   -------------------------
	
	private object AbsolutizingListener
		extends MouseButtonStateListener2 with MouseMoveListener2 with MouseWheelListener2 with MouseDragListener2
	{
		// IMPLEMENTED  -----------------
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent2] = AcceptAll
		override def mouseMoveEventFilter: Filter[MouseMoveEvent2] = AcceptAll
		override def mouseWheelEventFilter: Filter[MouseWheelEvent2] = AcceptAll
		override def mouseDragEventFilter: Filter[MouseDragEvent2] = AcceptAll
		
		// Removes the relative component from incoming events
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent2): ConsumeChoice = {
			// Also updates the mouse button states
			_buttonStates += event
			buttonHandler.onMouseButtonStateEvent(absolutize(event))
		}
		override def onMouseMove(event: MouseMoveEvent2): Unit = moveHandler.onMouseMove(absolutize(event))
		override def onMouseWheelRotated(event: MouseWheelEvent2): ConsumeChoice =
			wheelHandler.onMouseWheelRotated(absolutize(event))
		override def onMouseDrag(event: MouseDragEvent2): Unit = dragHandler.onMouseDrag(absolutize(event))
		
		
		// OTHER    --------------------
		
		private def absolutize[E](event: MouseEvent2[E]) = event.mapPosition { _.onlyAbsolute }
	}
}
