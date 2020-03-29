package utopia.genesis.event

import utopia.flow.util.TimeExtensions._
import utopia.genesis.shape.LinearVelocity
import utopia.inception.util.Filter
import utopia.genesis.shape.shape2D.Point
import utopia.genesis.shape.shape2D.Area2D

import scala.concurrent.duration.FiniteDuration

object MouseMoveEvent
{
    // OTHER METHODS    ------------
    
    /**
     * Creates an event filter that only accepts mouse events originating from the mouse entering 
     * the specified area
      * @param getArea A function for calculating the target area. Will be called each time an event needs to be filtered
     */
    def enterAreaFilter(getArea: => Area2D): Filter[MouseMoveEvent] = e => e.enteredArea(getArea)
    
    /**
     * Creates an event filter that only accepts mouse events originating from the mouse exiting the
     * specified area
      * @param getArea A function for calculating the target area. Will be called each time an event needs to be filtered.
     */
    def exitedAreaFilter(getArea: => Area2D): Filter[MouseMoveEvent] = e => e.exitedArea(getArea)
    
    /**
     * Creates an event filter that only accepts events where the mouse cursor moved with enough
     * speed
     */
    def minVelocityFilter(minVelocity: LinearVelocity): Filter[MouseMoveEvent] = e => e.velocity.linear >= minVelocity
}

/**
 * These events are generated when the mouse cursor moves
  * @param mousePosition The current mouse position
  * @param previousMousePosition The previous mouse position
  * @param buttonStatus Current mouse button status
  * @param duration The duration of the event (similar to act(...))
 * @author Mikko Hilpinen
 * @since 10.1.2017
 */
case class MouseMoveEvent(mousePosition: Point, previousMousePosition: Point, buttonStatus: MouseButtonStatus,
                          duration: FiniteDuration) extends MouseEvent[MouseMoveEvent]
{
    // COMPUTED PROPERTIES    -----------
    
    /**
     * The movement vector for the mouse cursor for the duration of the event
     */
    def transition = (mousePosition - previousMousePosition).toVector
    
    /**
     * The velocity vector of the mouse cursor (in pixels)
     */
    def velocity = transition.traversedIn(duration)
    
    /**
     * The duration of this event in duration format
     */
    def durationMillis = duration.toPreciseMillis
    
    
    // IMPLEMENTED  ---------------------
    
    def mapPosition(f: Point => Point) = copy(mousePosition = f(mousePosition),
        previousMousePosition = f(previousMousePosition))
    
    
    // OTHER METHODS    -----------------
    
    /**
     * Checks whether the mouse position was previously over a specified area
     */
    def wasOverArea(area: Area2D) = area.contains(previousMousePosition)
    
    /**
     * Checks whether the mouse cursor just entered a specified area
     */
    def enteredArea(area: Area2D) = !wasOverArea(area) && isOverArea(area)
    
    /**
     * Checks whether the mouse cursor just exited a specified area
     */
    def exitedArea(area: Area2D) = wasOverArea(area) && !isOverArea(area)
}