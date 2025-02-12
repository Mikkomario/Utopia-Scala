package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.{DelegatingLogger, Logger, SysErrLogger}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.template.{Handleable, Handlers}

import scala.collection.mutable

/**
  * Used for distributing mouse events inside the application, regardless of the active window.
  * This represents kind of the root mouse event source.
  * @author Mikko Hilpinen
  * @since 12.9.2020, v2.4
  */
object CommonMouseEvents extends mutable.Growable[Handleable]
{
	// ATTRIBUTES	--------------------------------
	
	private val logP = Pointer[Logger](SysErrLogger)
	private implicit val log: Logger = DelegatingLogger(logP)
	
	private var generators = Set[MouseEventGenerator]()
	private var _buttonStates = MouseButtonStates.default
	
	private val buttonHandler = MouseButtonStateHandler.empty
	private val moveHandler = MouseMoveHandler.empty
	private val wheelHandler = MouseWheelHandler.empty
	private val dragHandler = MouseDragHandler.empty
	
	/**
	  * The handlers (button, move, wheel & drag) managed by this interface
	  */
	val handlers = Handlers(buttonHandler, moveHandler, wheelHandler, dragHandler)
	
	private val dragTracker = new DragTracker(AbsolutizingListener)
	
	
	// INITIAL CODE --------------------------------
	
	buttonHandler += dragTracker
	moveHandler += dragTracker
	
	
	// COMPUTED -----------------------------------
	
	/**
	  * @return Current mouse button states
	  */
	def buttonStates = _buttonStates
	
	
	// IMPLEMENTED  -------------------------------
	
	override def addOne(elem: Handleable) = {
		handlers += elem
		this
	}
	
	override def clear() = handlers.clear()
	
	
	// OTHER	------------------------------------
	
	/**
	  * Specifies the logging implementation to use in the managed handlers
	  * @param logger Logging implementation to utilize
	  */
	def specifyLogger(logger: Logger) = logP.value = logger
	
	/**
	  * Registers a new event generator to be used for generating global mouse events
	  * @param generator A new mouse event generator
	  */
	def addGenerator(generator: MouseEventGenerator) = {
		if (!generators.contains(generator) && generator.hasNotStopped) {
			generators = generators + generator
			generator.handlers += AbsolutizingListener
			
			generator.stoppedFlag.onceSet { removeGenerator(generator) }
		}
	}
	@deprecated("Please use .addGenerator(MouseEventGenerator2) instead", "v4.0")
	def registerGenerator(generator: MouseEventGenerator) = addGenerator(generator)
	/**
	  * Removes a mouse event generator from the tracked / listened generators
	  * @param generator A generator to remove
	  */
	def removeGenerator(generator: MouseEventGenerator) = {
		if (generators.contains(generator)) {
			generator.handlers -= AbsolutizingListener
			generators -= generator
		}
	}
	@deprecated("Please use .removeGenerator(MouseEventGenerator) instead", "v4.0")
	def unregisterGenerator(generator: MouseEventGenerator) = removeGenerator(generator)
	
	/**
	  * Registers a new mouse button listener to this handler
	  * @param listener A new mouse button listener
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def registerButtonListener(listener: MouseButtonStateListener) =
		buttonHandler += listener
	/**
	  * Registers a new mouse move listener to this handler
	  * @param listener A mouse move listener
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def registerMoveListener(listener: MouseMoveListener) =
		moveHandler += listener
	/**
	  * Registers a new mouse wheel listener to this handler
	  * @param listener A mouse wheel listener
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def registerWheelListener(listener: MouseWheelListener) =
		wheelHandler += listener
	/**
	 * @param listener A new mouse drag listener to inform about mouse drag events
	 */
	@deprecated("Deprecated for removal", "v4.0")
	def registerDragListener(listener: MouseDragListener) =
		dragHandler += listener
	
	/**
	  * Registers a mouse event generator or a mouse related listener. If the specified instance is neither of these,
	  * does nothing
	  * @param item A generator or listener to register
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def register(item: Any) = item match {
		case generator: MouseEventGenerator => registerGenerator(generator)
		case listener: Handleable => handlers += listener
		case _ => ()
	}
	/**
	  * Removes a mouse event generator of a mouse related listener from this system
	  * @param item A generator or listener to remove
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def unregister(item: Any) = item match {
		case generator: MouseEventGenerator => unregisterGenerator(generator)
		case listener: Handleable => handlers -= listener
		case _ => ()
	}
	
	/**
	  * Removes a mouse event listener
	  * @param listener A listener that will no longer receive mouse-related events
	  */
	def -=(listener: Handleable) = handlers -= listener
	/**
	  * Removes 0-n mouse event listeners
	  * @param listeners Listeners to remove from receiving mouse-related events
	  */
	def --=(listeners: IterableOnce[Handleable]) = handlers --= listeners
	
	
	// NESTED   -------------------------
	
	private object AbsolutizingListener
		extends MouseButtonStateListener with MouseMoveListener with MouseWheelListener with MouseDragListener
	{
		// IMPLEMENTED  -----------------
		
		override def handleCondition: Flag = AlwaysTrue
		
		override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] = AcceptAll
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		override def mouseWheelEventFilter: Filter[MouseWheelEvent] = AcceptAll
		override def mouseDragEventFilter: Filter[MouseDragEvent] = AcceptAll
		
		// Removes the relative component from incoming events
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = {
			// Also updates the mouse button states
			_buttonStates += event
			buttonHandler.onMouseButtonStateEvent(absolutize(event))
		}
		override def onMouseMove(event: MouseMoveEvent): Unit = moveHandler.onMouseMove(absolutize(event))
		override def onMouseWheelRotated(event: MouseWheelEvent): ConsumeChoice =
			wheelHandler.onMouseWheelRotated(absolutize(event))
		override def onMouseDrag(event: MouseDragEvent): Unit = dragHandler.onMouseDrag(absolutize(event))
		
		
		// OTHER    --------------------
		
		private def absolutize[E](event: MouseEvent[E]) = event.mapPosition { _.onlyAbsolute }
	}
}
