package utopia.genesis.handling

import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.shape.shape2D.Point

import scala.concurrent.duration.FiniteDuration

/**
 * This mouse event listener is interested to continually receive events while the mouse cursor is
 * hovering over the instance. In order to work, the listener needs to receive both mouse movement
 * and action events
 */
trait MouseOverListener extends MouseMoveListener with Actor
{
    // ATTRIBUTES    -----------
    
    private var _mousePosition = Point.origin
    /**
     * The last mouse coordinates recorded by this listener
     */
    protected def mousePosition = _mousePosition
    
    
    // ABSTRACT ------------------
    
    /**
     * This method will be repeatedly called while the mouse cursor remains over a specified area
     * @param duration The duration since the last update
     */
    def onMouseOver(duration: FiniteDuration)
    
    /**
     * This method is used for determining whether a specified coordinate is considered to be
     * 'over' this instance.
     * @param position a position that is tested
     */
    def contains(position: Point): Boolean
    
    
    // IMPLEMENTED  ---------------
    
    override def onMouseMove(event: MouseMoveEvent) = _mousePosition = event.mousePosition
    
    override def act(duration: FiniteDuration) = if (contains(mousePosition)) onMouseOver(duration)
}
