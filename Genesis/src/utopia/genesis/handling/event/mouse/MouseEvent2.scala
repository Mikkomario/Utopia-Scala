package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.Filter
import utopia.genesis.event.MouseButton
import utopia.genesis.event.MouseButton.Middle
import utopia.paradigm.enumeration.OriginType
import utopia.paradigm.enumeration.OriginType.Relative
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.{Point, RelativePoint}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

object MouseEvent2
{
    // TYPES    ----------------------
    
    /**
      * A filter applied to any kind of mouse events
      */
    type MouseEventFilter = Filter[MouseEvent2[_]]
    
    
    // COMPUTED ---------------------
    
    /**
      * @return An access point for constructing mouse event filters
      */
    def filter = MouseEventFilter
    
    /**
    * This filter only accepts mouse events where the left mouse button is pressed down
    */
    @deprecated("Please use .filter.whileLeftDown instead", "v3.6")
    def isLeftDownFilter = filter.whileLeftDown
    /**
      * This filter only accepts mouse events where the right mouse button is pressed down
      */
    @deprecated("Please use .filter.whileRightDown instead", "v3.6")
    def isRightDownFilter = filter.whileRightDown
    /**
      * This filter only accepts mouse events where the middle mouse button is pressed down
      */
    @deprecated("Please use .filter.whileMiddleDown instead", "v3.6")
    def isMiddleDownFilter = filter.whileMiddleDown
    
    
    // OTHER    --------------------
    
    /**
      * This filter only accepts mouse events where the mouse cursor is over the specified area
      * @param getArea A function for calculating the target area. Will be called each time an event is being filtered.
      */
    @deprecated("Please use .filter.over(Area2D) instead", "v3.6")
    def isOverAreaFilter(getArea: => Area2D) = filter.over(getArea)
    /**
      * @param area Tested area (call-by-name)
      * @return A filter that only accepts events that occur outside the specified area
      */
    @deprecated("Please use .filter.outside(Area2D) instead", "v3.6")
    def isOutsideAreaFilter(area: => Area2D) = filter.outside(area)
    
    /**
      * This filter only accepts events where a mouse button with the specified index has the
      * specified status (down (true) or up (false))
      */
    @deprecated("Deprecated for removal. Use filter.whileButtonDown(MouseButton) or .whileButtonReleased(MouseButton) instead.", "v3.6")
    def buttonStatusFilter(buttonIndex: Int, requiredStatus: Boolean) =
        filter { _.buttonStates(buttonIndex) == requiredStatus }
    /**
      * This filter only accepts events where a mouse button has the
      * specified status (down (true) or up (false))
      * @param requiredStatus The status the button must have in order for the event to be included.
      * Defaults to true (down)
      */
    @deprecated("Deprecated for removal. Use filter.whileButtonDown(MouseButton) or .whileButtonReleased(MouseButton) instead.", "v3.6")
    def buttonStatusFilter(button: MouseButton, requiredStatus: Boolean = true) =
        filter { _.buttonStates(button) == requiredStatus }
    
    
    // NESTED   ---------------------
    
    trait MouseFilteringFactory[+E <: MouseEvent2[_], +Repr]
    {
        // ABSTRACT -----------------
        
        /**
          * @param filter A new filter to apply
          * @return Copy of this item with the specified filter applied
          */
        protected def withFilter(filter: Filter[E]): Repr
        
        
        // COMPUTED -----------------
        
        /**
          * @return An item that only accepts events while the left mouse button is pressed
          */
        def whileLeftDown = whileButtonDown(MouseButton.Left)
        /**
          * @return An item that only accepts events while the right mouse button is pressed
          */
        def whileRightDown = whileButtonDown(MouseButton.Right)
        /**
          * @return An item that only accepts events while the middle mouse button is pressed
          */
        def whileMiddleDown = whileButtonDown(Middle)
        
        
        // OTHER    -----------------
        
        /**
          * @param area Targeted relative area (call-by-name)
          * @return An item that only accepts events that occur over the specified relative area
          */
        def over(area: => Area2D) = withFilter { _.isOver(area) }
        /**
          * @param area Targeted relative area (call-by-name)
          * @return An item that only accepts events that occur outside the specified relative area
          */
        def outside(area: => Area2D) = withFilter { _.isOutside(area) }
        
        /**
          * @param button Targeted button
          * @return An item that only accepts events while the specified button is pressed
          */
        def whileButtonDown(button: MouseButton) = withFilter { _.buttonStates(button) }
        /**
          * @param button Targeted button
          * @return An item that only accepts events while the specified button is in the released state
          */
        def whileButtonReleased(button: MouseButton) = withFilter { !_.buttonStates(button) }
    }
    
    object MouseEventFilter extends MouseFilteringFactory[MouseEvent2[_], MouseEventFilter]
    {
        // IMPLEMENTED  ---------------
        
        override protected def withFilter(filter: Filter[MouseEvent2[_]]): MouseEventFilter = filter
        
        
        // OTHER    -------------------
        
        /**
          * @param f A filter function for mouse events
          * @return A filter based on that function
          */
        def apply(f: MouseEvent2[_] => Boolean): MouseEventFilter = Filter(f)
    }
}

/**
 * This trait contains the common properties shared between different mouse event types
 * @author Mikko Hilpinen
 * @since 19.2.2017
 */
trait MouseEvent2[+Repr]
{
    // ABSTRACT ----------------------
    
    /**
      * @return Mouse position relative to this event's context.
      *         Typically events are relative to a containing component's top left corner.
      */
    def position: RelativePoint
    
    /**
      * @return Mouse button states immediately after this event
      */
    def buttonStates: MouseButtonStates
    
    /**
      * @param position New position to assign
      * @return Copy of this event with the specified position
      */
    def withPosition(position: RelativePoint): Repr
    
    
    // COMPUTED ----------------------
    
    /**
      * @return The current (relative) mouse position
      */
    @deprecated("Please use .position instead", "v3.6")
    def mousePosition: Point = position.relative
    /**
      * @return Mouse position in the current screen coordinate system (in pixels)
      */
    @deprecated("Please use .position.absolute instead", "v3.6")
    def absoluteMousePosition: Point = position.absolute
    
    /**
      * @return The current mouse button status
      */
    @deprecated("Please use .buttonStates instead", "v3.6")
    def buttonStatus = buttonStates
    
    
    // OTHER    ----------------------
    
    /**
      * @param area Targeted area
      * @param areaType Whether the specified area is absolute or relative (default)
      * @return Whether the mouse cursor is currently over the specified area
      */
    def isOver(area: Area2D, areaType: OriginType = Relative) = area.contains(position(areaType))
    /**
     * Checks whether the mouse cursor is currently over the specified (relative) area
     */
    @deprecated("Please use .isOver(Area2D) instead", "v3.6")
    def isOverArea(area: Area2D) = isOver(area)
    /**
      * @param area Targeted relative area
      * @param areaType Whether the specified area is absolute or relative (default)
      * @return Whether this event's mouse position is outside of the specified area
      */
    def isOutside(area: Area2D, areaType: OriginType = Relative) = !isOver(area, areaType)
    /**
      * @param area an area (relative)
      * @return Whether the mouse is currently outside of that area
      */
    @deprecated("Please use .isOutside(Area2D) instead", "v3.6")
    def isOutsideArea(area: Area2D) = isOutside(area)
    
    /**
      * @param area Target area
      * @return Mouse position relative to the specified area
      */
    @deprecated("Deprecated for removal", "v3.6")
    def positionOverArea(area: Bounds) = mousePosition - area.position
    
    /**
      * @param f A mapping function for this event's position
      * @return Copy of this event with mapped position
      */
    def mapPosition(f: RelativePoint => RelativePoint) = withPosition(f(position))
    
    /**
      * @param transition Amount of translation applied to this event's position
      * @return A copy of this event with translated position
      */
    def translated(transition: HasDoubleDimensions) = mapPosition { _ + transition }
    
    /**
      * @param origin New origin
      * @param originType Whether the specified origin is in the same relative space as this event (default),
      *                   or whether it is in the absolute space.
      * @return A copy of this event where positions are relative to the specified origin.
      *         The absolute position is preserved.
      */
    def relativeTo(origin: Point, originType: OriginType = Relative) =
        mapPosition { _.relativeTo(origin, originType) }
}