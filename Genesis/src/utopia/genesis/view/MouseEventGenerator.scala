package utopia.genesis.view

import java.awt.{Component, MouseInfo}
import java.awt.event.{MouseEvent, MouseListener, MouseWheelListener}
import utopia.flow.async.ActionQueue
import utopia.genesis.event.{MouseButtonStateEvent, MouseButtonStatus, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.handling.mutable.{MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.handling.Actor
import utopia.paradigm.shape.shape2d.Point
import utopia.inception.handling.mutable.Killable
import utopia.inception.handling.{HandlerType, Mortal}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.ref.WeakReference
import scala.util.Try

/**
 * This class listens to mouse status inside a component and generates new mouse events. This
 * class implements the Actor trait so in order to work, it should be added to a working
 * actorHandler.
 * @author Mikko Hilpinen
 * @since 22.1.2017
 */
class MouseEventGenerator(c: Component, scaling: => Double = 1.0)(implicit exc: ExecutionContext)
    extends Actor with Mortal with Killable
{
    // ATTRIBUTES    -----------------
    
    // Generated events are fired one at a time using an event queue
    private val eventQueue = new ActionQueue()
    
    private val componentPointer = WeakReference(c)
    
    private val _moveHandler = VolatileOption[MouseMoveHandler]()
    private val _buttonHandler = VolatileOption[MouseButtonStateHandler]()
    private val _wheelHandler = VolatileOption[MouseWheelHandler]()
    
    private var lastMousePosition = Point.origin
    private var lastAbsoluteMousePosition = Point.origin
    private var buttonStatus = MouseButtonStatus.empty
    
    
    // COMPUTED ----------------------
    
    /**
      * @return A handler that distributes generated mouse move events
      */
    def moveHandler = _moveHandler.setOneIfEmpty { MouseMoveHandler() }
    /**
      * @return A handler that distributes generated mouse button state events
      */
    def buttonHandler = _buttonHandler.setOneIfEmpty {
        component.foreach { _.addMouseListener(MouseEventReceiver) }
        MouseButtonStateHandler()
    }
    /**
      * @return A handler that distributes generated mouse wheel events
      */
    def wheelHandler = _wheelHandler.setOneIfEmpty {
        component.foreach { _.addMouseWheelListener(MouseWheelEventReceiver) }
        MouseWheelHandler()
    }
    
    private def component = componentPointer.get
    
    
    // IMPLEMENTED METHODS    --------
    
    // This generator dies once component is no longer reachable
    override def isDead = component.isEmpty
    
    override def kill() =
    {
        component.foreach { c =>
            c.removeMouseListener(MouseEventReceiver)
            c.removeMouseWheelListener(MouseWheelEventReceiver)
            componentPointer.clear()
            _moveHandler.clear()
            _buttonHandler.clear()
            _wheelHandler.clear()
        }
    }
    
    // Allows handling when component is visible
    override def allowsHandlingFrom(handlerType: HandlerType) = component.exists { _.isShowing }
    
    override def act(duration: FiniteDuration) =
    {
        component.foreach { c =>
            // Checks for mouse movement
            // Sometimes mouse position can't be calculated, in which case assumes mouse to remain static
            Try { Option(MouseInfo.getPointerInfo) }.toOption.flatten.foreach { pointerInfo =>
                val absoluteMousePosition = Point of pointerInfo.getLocation
                val mousePosition = pointInPanel(absoluteMousePosition, c) / scaling
                if (mousePosition != lastMousePosition)
                {
                    val previousMousePosition = lastMousePosition
                    lastMousePosition = mousePosition
                    lastAbsoluteMousePosition = absoluteMousePosition
                    // Informs the handler only if one has been generated
                    _moveHandler.foreach { handler =>
                        val event = new MouseMoveEvent(mousePosition, previousMousePosition, absoluteMousePosition,
                            buttonStatus, duration)
                        eventQueue.push { handler.onMouseMove(event) }
                    }
                }
            }
        }
    }
    
    
    // OTHER METHODS    --------------
    
    /**
      * Simulates the release of all currently held-down mouse buttons
      */
    def releaseAllKeys() =
    {
        // Updates local button status
        if (buttonStatus.isAnyButtonPressed)
        {
            val oldButtonStatus = buttonStatus
            buttonStatus = MouseButtonStatus.empty
    
            // Informs attached listeners for each released key
            _buttonHandler.foreach { handler =>
                oldButtonStatus.downButtons.foreach { releasedButton =>
                    val event = MouseButtonStateEvent(releasedButton.buttonIndex, isDown = false, lastMousePosition,
                        lastAbsoluteMousePosition, buttonStatus)
                    eventQueue.push { handler.onMouseButtonState(event) }
                }
            }
        }
    }
    
    @scala.annotation.tailrec
    private def pointInPanel(point: Point, panel: Component): Point =
    {
        val relativePoint = point - (Point of panel.getLocation)
        
        panel match
        {
            case _: java.awt.Window => relativePoint
            case _ =>
                val parent = panel.getParent
                if (parent == null) relativePoint else pointInPanel(relativePoint, parent)
        }
    }
    
    
    // NESTED CLASSES    ------------
    
    private object MouseEventReceiver extends MouseListener
    {
        // IMPLEMENTED  -------------
        
        // TODO: Use an action queue in the simulated events to make sure release events are distributed AFTER the press events
        override def mousePressed(e: MouseEvent) =  distributeEvent(e, isDown = true)
        override def mouseReleased(e: MouseEvent) = distributeEvent(e, isDown = false)
        
        override def mouseClicked(e: MouseEvent) = ()
        override def mouseEntered(e: MouseEvent) = ()
        override def mouseExited(e: MouseEvent) = ()
        
        
        // OTHER    ---------------
        
        private def distributeEvent(event: MouseEvent, isDown: Boolean) =
        {
            buttonStatus += (event.getButton, isDown)
            _buttonHandler.foreach { handler =>
                val newEvent = MouseButtonStateEvent(event.getButton, isDown, lastMousePosition,
                    lastAbsoluteMousePosition, buttonStatus)
                // Distributes the event asynchronously
                eventQueue.push { handler.onMouseButtonState(newEvent) }
            }
        }
    }
    
    private object MouseWheelEventReceiver extends MouseWheelListener
    {
        override def mouseWheelMoved(e: java.awt.event.MouseWheelEvent) =
            _wheelHandler.foreach { handler =>
                val event = MouseWheelEvent(e.getWheelRotation, lastMousePosition, lastAbsoluteMousePosition,
                    buttonStatus)
                // Distributes the event asynchronously
                eventQueue.push { handler.onMouseWheelRotated(event) }
            }
    }
}