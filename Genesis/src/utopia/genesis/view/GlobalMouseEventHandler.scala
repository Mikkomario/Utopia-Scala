package utopia.genesis.view

import utopia.flow.view.mutable.Pointer
import utopia.genesis.event.{ConsumeEvent, KeyStatus, MouseButton, MouseButtonStateEvent, MouseDragEvent, MouseMoveEvent}
import utopia.genesis.handling.{MouseButtonStateListener, MouseDragListener, MouseMoveListener, MouseWheelListener, mutable}
import utopia.inception.handling.{Handleable, HandlerType}
import utopia.inception.handling.mutable.HandlerRelay
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

/**
  * Used for distributing "global" mouse events inside the application, regardless of active window.
  * @author Mikko Hilpinen
  * @since 12.9.2020, v2.4
  */
object GlobalMouseEventHandler
{
	// ATTRIBUTES	--------------------------------
	
	private var generators = Set[MouseEventGenerator]()
	
	private val buttonHandler = mutable.MouseButtonStateHandler()
	private val moveHandler = mutable.MouseMoveHandler()
	private lazy val wheelHandler = mutable.MouseWheelHandler()
	private val dragHandler = mutable.MouseDragHandler()
	
	private lazy val handlers = HandlerRelay(buttonHandler, moveHandler, wheelHandler, dragHandler)
	
	private val dragTracker = new DragTracker(dragHandler)
	
	
	// INITIAL CODE --------------------------------
	
	buttonHandler += dragTracker
	moveHandler += dragTracker
	
	
	// COMPUTED	------------------------------------
	
	/**
	  * @return A string representation of the currently handled items
	  */
	def debugString = handlers.debugString
	
	
	// OTHER	------------------------------------
	
	/**
	  * Registers a new event generator to be used for generating global mouse events
	  * @param generator A new mouse event generator
	  */
	def registerGenerator(generator: MouseEventGenerator) =
	{
		if (generator.isAlive)
		{
			generators = generators.filter { _.isAlive } + generator
			generator.buttonHandler += buttonHandler
			generator.moveHandler += moveHandler
			generator.wheelHandler += wheelHandler
		}
	}
	
	/**
	  * Removes a mouse event generator from the tracked / listened generators
	  * @param generator A generator to remove
	  */
	def unregisterGenerator(generator: MouseEventGenerator) =
	{
		if (generators.contains(generator))
		{
			if (generator.isAlive)
			{
				generators = (generators - generator).filter { _.isAlive }
				generator.buttonHandler -= buttonHandler
				generator.moveHandler -= moveHandler
				generator.wheelHandler -= wheelHandler
			}
			else
				generators = generators.filter { _.isAlive }
		}
	}
	
	/**
	  * Registers a new mouse button listener to this handler
	  * @param listener A new mouse button listener
	  */
	def registerButtonListener(listener: MouseButtonStateListener) = buttonHandler += listener
	/**
	  * Registers a new mouse move listener to this handler
	  * @param listener A mouse move listener
	  */
	def registerMoveListener(listener: MouseMoveListener) = moveHandler += listener
	/**
	  * Registers a new mouse wheel listener to this handler
	  * @param listener A mouse wheel listener
	  */
	def registerWheelListener(listener: MouseWheelListener) = wheelHandler += listener
	/**
	 * @param listener A new mouse drag listener to inform about mouse drag events
	 */
	def registerDragListener(listener: MouseDragListener) = dragHandler += listener
	
	/**
	  * Registers a mouse event generator or a mouse related listener. If the specified instance is neither of these,
	  * does nothing
	  * @param item A generator or listener to register
	  */
	def register(item: Any) = item match {
		case generator: MouseEventGenerator => registerGenerator(generator)
		case listener: Handleable => handlers += listener
		case _ => ()
	}
	/**
	  * Removes a mouse event generator of a mouse related listener from this system
	  * @param item A generator or listener to remove
	  */
	def unregister(item: Any) = item match {
		case generator: MouseEventGenerator => unregisterGenerator(generator)
		case listener: Handleable => handlers -= listener
		case _ => ()
	}
	
	/**
	  * Registers a new mouse event listener
	  * @param listener A listener that will receive new mouse-related events
	  */
	def +=(listener: Handleable) = handlers += listener
	/**
	  * Removes a mouse event listener
	  * @param listener A listener that will no longer receive mouse-related events
	  */
	def -=(listener: Handleable) = handlers -= listener
}
