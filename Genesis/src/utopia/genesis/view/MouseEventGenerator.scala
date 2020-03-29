package utopia.genesis.view

import java.awt.{Component, MouseInfo}
import java.awt.event.{MouseEvent, MouseListener, MouseWheelListener}

import utopia.genesis.event.{MouseButtonStateEvent, MouseButtonStatus, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.handling.{Actor, MouseButtonStateListener, MouseMoveListener}
import utopia.genesis.shape.shape2D.Point
import utopia.inception.handling.{HandlerType, Mortal}

import scala.concurrent.duration.FiniteDuration
import scala.ref.WeakReference

/**
 * This class listens to mouse status inside a component and generates new mouse events. This
 * class implements the Actor trait so in order to work, it should be added to a working
 * actorHandler.
 * @author Mikko Hilpinen
 * @since 22.1.2017
 */
class MouseEventGenerator(c: Component, val moveHandler: MouseMoveListener,
                          val buttonHandler: MouseButtonStateListener,
                          val wheelHandler: utopia.genesis.handling.MouseWheelListener,
                          private val getScaling: () => Double) extends Actor with Mortal
{
    // ATTRIBUTES    -----------------
    
    private val component = WeakReference(c)
    
    private var lastMousePosition = Point.origin
    private var buttonStatus = MouseButtonStatus.empty
    
    
    // INITIAL CODE    ---------------
    
    // Starts listening for mouse events inside the component
    c.addMouseListener(new MouseEventReceiver())
    c.addMouseWheelListener(new MouseWheelEventReceiver())
    
    
    // IMPLEMENTED METHODS    --------
    
    override def parent = None
    
    // This generator dies once component is no longer reachable
    override def isDead = component.get.isEmpty
    
    // Allows handling when component is visible
    override def allowsHandlingFrom(handlerType: HandlerType) = component.get.exists { _.isShowing }
    
    override def act(duration: FiniteDuration) =
    {
        component.get.foreach
        {
            c =>
                // Checks for mouse movement
                val mousePosition = pointInPanel(Point of MouseInfo.getPointerInfo.getLocation, c) / getScaling()
                
                if (mousePosition != lastMousePosition)
                {
                    val event = new MouseMoveEvent(mousePosition, lastMousePosition, buttonStatus, duration)
                    lastMousePosition = mousePosition
                    moveHandler.onMouseMove(event)
                }
        }
    }
    
    
    // OTHER METHODS    --------------
    
    @scala.annotation.tailrec
    private def pointInPanel(point: Point, panel: Component): Point =
    {
        val relativePoint = point - (Point of panel.getLocation)
        
        panel match
        {
            case w: java.awt.Window => relativePoint
            case _ =>
                val parent = panel.getParent
                if (parent == null) relativePoint else pointInPanel(relativePoint, parent)
        }
    }
    
    
    // NESTED CLASSES    ------------
    
    private class MouseEventReceiver extends MouseListener
    {
        override def mousePressed(e: MouseEvent) = 
        {
            buttonStatus += (e.getButton, true)
            buttonHandler.onMouseButtonState(new MouseButtonStateEvent(e.getButton, true, lastMousePosition,
                buttonStatus))
        }
        
        override def mouseReleased(e: MouseEvent) = 
        {
            buttonStatus += (e.getButton, false)
            buttonHandler.onMouseButtonState(new MouseButtonStateEvent(e.getButton, false, lastMousePosition,
                buttonStatus))
        }
        
        override def mouseClicked(e: MouseEvent) = Unit
        override def mouseEntered(e: MouseEvent) = Unit
        override def mouseExited(e: MouseEvent) = Unit
    }
    
    private class MouseWheelEventReceiver extends MouseWheelListener
    {
        override def mouseWheelMoved(e: java.awt.event.MouseWheelEvent) = 
                wheelHandler.onMouseWheelRotated(MouseWheelEvent(e.getWheelRotation, lastMousePosition, buttonStatus))
    }
}