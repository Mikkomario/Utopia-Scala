package utopia.genesis.event

import utopia.inception.util.Filter
import utopia.genesis.event.MouseButton._
import utopia.genesis.shape.VectorLike
import utopia.genesis.shape.shape2D.{Area2D, Bounds, Point}

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
trait MouseEvent[+Repr]
{
    // ABSTRACT ----------------------
    
    /**
      * @return The current position of the mouse
      */
    def mousePosition: Point
    
    /**
      * @return The current mouse button status
      */
    def buttonStatus: MouseButtonStatus
    
    /**
      * @param f A mapping function for this event's position
      * @return A copy of this event with mapped position
      */
    def mapPosition(f: Point => Point): Repr
    
    
    // OTHER    ----------------------
    
    /**
     * Checks whether the mouse cursor is currently over the specified area
     */
    def isOverArea(area: Area2D) = area.contains(mousePosition)
    
    /**
      * @param area Target area
      * @return Mouse position relative to the specified area
      */
    def positionOverArea(area: Bounds) = mousePosition - area.position
    
    /**
      * @param amount Amount of translation applied to this event's position
      * @return A copy of this event with translated position
      */
    def translated(amount: VectorLike[_]) = mapPosition { _ + amount }
    
    /**
      * @param origin New origin
      * @return A copy of this event where positions are relative to the specified origin
      */
    def relativeTo(origin: Point) = translated(-origin)
}