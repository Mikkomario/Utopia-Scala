package utopia.genesis.handling

import utopia.genesis.event.{ConsumeEvent, MouseWheelEvent}
import utopia.genesis.shape.shape2D.Area2D
import utopia.inception.handling.Handleable
import utopia.inception.util.{AnyFilter, Filter}

object MouseWheelListener
{
    /**
      * Creates a new mouse wheel listener that calls specified function on wheel rotations
      * @param filter A filter that specifies, which events will trigger the function (default = no filtering)
      * @param f A function that will be called on wheel rotations
      * @return A new mouse event listener
      */
    def apply(filter: Filter[MouseWheelEvent] = AnyFilter)(f: MouseWheelEvent => Option[ConsumeEvent]): MouseWheelListener =
        new FunctionalMouseWheelListener(f, filter)
    
    /**
      * Creates a new mouse wheel listener that calls specified function on wheel rotations inside a specific area
      * @param getArea A function for getting the target area
      * @param f A function that will be called on wheel rotations
      * @return A new mouse event listener
      */
    def onWheelInsideArea(getArea: => Area2D)(f: MouseWheelEvent => Option[ConsumeEvent]) =
        apply { e => e.isOverArea(getArea) }(f)
    
    /**
      * Creates a new mouse wheel listener that calls specified function on wheel rotations inside a specific area
      * @param getArea A function for getting the target area
      * @param f A function that will be called on wheel rotations
      * @return A new mouse event listener
      */
    def onWheelInsideAreaNoConsume(getArea: => Area2D)(f: MouseWheelEvent => Unit) =
        onWheelInsideArea(getArea) { e => f(e); None }
}

/**
 * This trait is implemented by classes which are interested in being notified when the mouse wheel
 * is rotated
 */
trait MouseWheelListener extends Handleable
{
    /**
     * This method is called whenever the mouse wheel rotates
      * @return If the event was consumed during this process, should return the consume event
     */
    def onMouseWheelRotated(event: MouseWheelEvent): Option[ConsumeEvent]
    
    /**
     * This filter is applied to the events incoming to the listener. Only events accepted by this
     * filter are informed to the instance. The default filter accepts all mouse wheel events.
     */
    def mouseWheelEventFilter: Filter[MouseWheelEvent] = AnyFilter
    
    /**
      * @return Whether this instance is willing to receive mouse wheel events
      */
    def isReceivingMouseWheelEvents = allowsHandlingFrom(MouseWheelHandlerType)
}

private class FunctionalMouseWheelListener(val f: MouseWheelEvent => Option[ConsumeEvent], val filter: Filter[MouseWheelEvent])
    extends MouseWheelListener with utopia.inception.handling.immutable.Handleable
{
    override def onMouseWheelRotated(event: MouseWheelEvent) = f(event)
    
    override def mouseWheelEventFilter = filter
}