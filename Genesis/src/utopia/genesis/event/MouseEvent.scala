package utopia.genesis.event

import utopia.genesis.handling.event.mouse.MouseButton
import utopia.inception.util.Filter
import utopia.genesis.handling.event.mouse.MouseButton._
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.{Point, RelativePoint}
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
object MouseEvent
{
    /**
     * This filter only accepts mouse events where the left mouse button is pressed down
     */
    val isLeftDownFilter = buttonStatusFilter(Left)
    
    /**
     * This filter only accepts mouse events where the right mouse button is pressed down
     */
    val isRightDownFilter = buttonStatusFilter(Right)
    
    /**
     * This filter only accepts mouse events where the middle mouse button is pressed down
     */
    val isMiddleDownFilter = buttonStatusFilter(Middle)
    
    /**
     * This filter only accepts mouse events where the mouse cursor is over the specified area
      * @param getArea A function for calculating the target area. Will be called each time an event is being filtered.
     */
    def isOverAreaFilter(getArea: => Area2D): Filter[MouseEvent[Any]] = e => e.isOverArea(getArea)
    /**
      * @param area Tested area (call-by-name)
      * @return A filter that only accepts events that occur outside the specified area
      */
    def isOutsideAreaFilter(area: => Area2D): Filter[MouseEvent[Any]] = e => e.isOutsideArea(area)
    
    /**
     * This filter only accepts events where a mouse button with the specified index has the
     * specified status (down (true) or up (false))
     */
    def buttonStatusFilter(buttonIndex: Int, requiredStatus: Boolean): Filter[MouseEvent[Any]] =
        e => e.buttonStatus(buttonIndex) == requiredStatus
    
    /**
     * This filter only accepts events where a mouse button has the
     * specified status (down (true) or up (false))
     * @param requiredStatus The status the button must have in order for the event to be included.
     * Defaults to true (down)
     */
    def buttonStatusFilter(button: MouseButton, requiredStatus: Boolean = true): Filter[MouseEvent[Any]] =
        e => e.buttonStatus(button) == requiredStatus
}

/**
 * This trait contains the common properties shared between different mouse event types
 * @author Mikko Hilpinen
 * @since 19.2.2017
 */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
trait MouseEvent[+Repr]
{
    // ABSTRACT ----------------------
    
    // TODO: Instead of these two values, use a RelativePoint value
    /**
      * @return The current (relative) mouse position
      */
    def mousePosition: Point
    /**
      * @return Mouse position in the current screen coordinate system (in pixels)
      */
    def absoluteMousePosition: Point
    
    /**
      * @return The current mouse button status
      */
    def buttonStatus: MouseButtonStatus
    
    /**
      * @param f A mapping function for this event's position
      * @return A copy of this event with mapped position
      */
    // FIXME: This documentation is misleading. Doesn't map the absolute position.
    def mapPosition(f: Point => Point): Repr
    
    
    // COMPUTED ----------------------
    
    /**
     * @return The position where the mouse cursor is currently over
     */
    def position = RelativePoint(mousePosition, absoluteMousePosition)
    
    
    // OTHER    ----------------------
    
    /**
     * Checks whether the mouse cursor is currently over the specified (relative) area
     */
    def isOverArea(area: Area2D) = area.contains(mousePosition)
    /**
      * @param area an area (relative)
      * @return Whether the mouse is currently outside of that area
      */
    def isOutsideArea(area: Area2D) = !isOverArea(area)
    
    /**
      * @param area Target area
      * @return Mouse position relative to the specified area
      */
    def positionOverArea(area: Bounds) = mousePosition - area.position
    
    /**
      * @param amount Amount of translation applied to this event's position
      * @return A copy of this event with translated position
      */
    def translated(amount: HasDoubleDimensions) = mapPosition { _ + amount }
    
    /**
      * @param origin New origin
      * @return A copy of this event where positions are relative to the specified origin
      */
    // FIXME: This also is a misleading function, as it assumes an absolute starting state
    def relativeTo(origin: Point) = translated(-origin)
}