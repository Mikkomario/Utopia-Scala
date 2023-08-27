package utopia.genesis.event

import utopia.flow.collection.immutable.Pair
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.inception.util.Filter
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.vector.point.Point

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
      * @param area The followed area (call-by-name)
      * @return A filter that only accepts events where the mouse entered or exited the specified area
      */
    def enteredOrExitedAreaFilter(area: => Area2D): Filter[MouseMoveEvent] = { e =>
        val a = area
        Pair(e.mousePosition, e.previousMousePosition).isAsymmetricBy { a.contains(_) }
    }
    
    /**
     * Creates an event filter that only accepts events where the mouse cursor moved with enough
     * speed
     */
    def minVelocityFilter(minVelocity: LinearVelocity): Filter[MouseMoveEvent] = e => e.velocity.linear >= minVelocity
}

/**
 * These events are generated when the mouse cursor moves
  * @param mousePosition The current (relative) mouse position
  * @param previousMousePosition The previous (relative) mouse position
  * @param absoluteMousePosition current mouse position in screen coordinate system (in pixels)
  * @param buttonStatus Current mouse button status
  * @param duration The duration of the event (similar to act(...))
 * @author Mikko Hilpinen
 * @since 10.1.2017
 */
// TODO: Convert into a trait
case class MouseMoveEvent(override val mousePosition: Point, previousMousePosition: Point,
                          override val absoluteMousePosition: Point,
                          override val buttonStatus: MouseButtonStatus,
                          duration: FiniteDuration)
    extends MouseEvent[MouseMoveEvent]
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
    
    /**
      * @return Previously recorded absolute mouse position (meaning a position in the screen pixel coordinate system)
      */
    def previousAbsoluteMousePosition = absoluteMousePosition - transition
    
    
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