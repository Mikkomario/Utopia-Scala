package utopia.genesis.handling

import utopia.genesis.event.{MouseEvent, MouseMoveEvent}
import utopia.genesis.shape.shape2D.Area2D
import utopia.inception.handling.Handleable
import utopia.inception.util.{AnyFilter, Filter}

object MouseMoveListener
{
    /**
      * Creates a new mouse move listener that calls the specified function
      * @param filter A filter that determines which events trigger the function (default = no filtering)
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    def apply(filter: Filter[MouseMoveEvent] = AnyFilter)(f: MouseMoveEvent => Unit): MouseMoveListener =
        new FunctionalMouseMoveListener(f, filter)
    
    /**
      * Creates a new mouse move listener that calls specified function on drags (with left mouse button)
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    def onLeftDragged(f: MouseMoveEvent => Unit) = apply(MouseEvent.isLeftDownFilter)(f)
    
    /**
      * Creates a new mouse move listener that calls specified function on drags (with right mouse button)
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    def onRightDragged(f: MouseMoveEvent => Unit) = apply(MouseEvent.isRightDownFilter)(f)
    
    /**
      * Creates a new mouse move listener that calls specified function each time mouse enters specified area
      * @param getArea a function for calculating the target area
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    def onEnter(getArea: => Area2D)(f: MouseMoveEvent => Unit) = apply { e => e.enteredArea(getArea) }(f)
    
    /**
      * Creates a new mouse move listener that calls specified function each time mouse exits specified area
      * @param getArea a function for calculating the target area
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    def onExit(getArea: => Area2D)(f: MouseMoveEvent => Unit) = apply { e => e.exitedArea(getArea) }(f)
}

/**
 * MouseMoveListeners are interested in receiving mouse move events
 * @author Mikko Hilpinen
 * @since 21.1.2017
 */
trait MouseMoveListener extends Handleable
{
    /**
     * This filter is applied over mouse move events the listener would receive. Only events
     * accepted by the filter are informed to the listener. The default implementation accepts
     * all incoming events.
     */
    def mouseMoveEventFilter: Filter[MouseMoveEvent] = AnyFilter
    
    /**
     * This method is used for informing the listener of new mouse events. This method should
     * only be called for events that are accepted by the listener's filter.
     * @param event The event that occurred.
     */
    def onMouseMove(event: MouseMoveEvent)
    
    /**
      * @return Whether this instance is willing to receive mouse move events
      */
    def isReceivingMouseMoveEvents = allowsHandlingFrom(MouseMoveHandlerType)
}

private class FunctionalMouseMoveListener(val f: MouseMoveEvent => Unit, val filter: Filter[MouseMoveEvent])
    extends MouseMoveListener with utopia.inception.handling.immutable.Handleable
{
    override def onMouseMove(event: MouseMoveEvent) = f(event)
    
    override def mouseMoveEventFilter = filter
}
