package utopia.genesis.handling.event.mouse

import utopia.genesis.event.MouseButton._
import utopia.genesis.event.{Consumable, ConsumeEvent, MouseButton}
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

// TODO: Add filter access points (deprecated)

/**
 * Mouse button events are generated whenever a mouse button state changes
  * (i.e. when a mouse button is pressed or released)
  * @param button The mouse button that triggered this event
  * @param position Current mouse coordinates
  * @param buttonStates Mouse button states immediately after this event
  * @param consumeEvent An event concerning this event's consuming. None if not consumed.
  * @param pressed Whether this button was pressed (true) or released (false)
 * @author Mikko Hilpinen
 * @since 17.2.2017
 */
case class MouseButtonStateEvent2(button: MouseButton, override val position: RelativePoint,
                                  override val buttonStates: MouseButtonStates,
                                  override val consumeEvent: Option[ConsumeEvent], pressed: Boolean)
    extends MouseEvent2[MouseButtonStateEvent2] with Consumable[MouseButtonStateEvent2]
{
    // COMPUTED PROPERTIES    ------------
    
    /**
      * @return Whether the mouse button was released / whether this event was triggered by a mouse button release
      */
    def released = !pressed
    
    @deprecated("Please use .pressed instead", "v3.6")
    def isDown = pressed
    /**
      * @return Whether target mouse button is currently released / up
      */
    @deprecated("Please use .released instead", "v3.6")
    def isUp = !isDown
    /**
     * Whether the mouse button was just pressed down
     */
    @deprecated("Please use .pressed instead", "v3.6")
    def wasPressed = isDown
    /**
     * Whether the mouse button was just released from down state
     */
    @deprecated("Please use .released instead", "v3.6")
    def wasReleased = !isDown
    
    /**
      * @return Index of the button that triggered this event
      */
    @deprecated("Please use .button.index instead", "v3.6")
    def buttonIndex = button.index
    
    /**
      * @return Whether this event was triggered by the left mouse button
      */
    def concernsLeft = concerns(Left)
    /**
      * @return Whether this event was triggered by the right mouse button
      */
    def concernsRight = concerns(Right)
    /**
      * @return Whether this event was triggered by the middle mouse button
      */
    def concernsMiddle = concerns(Middle)
    
    /**
     * Whether this event concerns the left mouse button
     */
    @deprecated("Please use .concernsLeft instead", "v3.6")
    def isLeftMouseButton = isMouseButton(Left)
    /**
     * Whether this event concerns the right mouse button
     */
    @deprecated("Please use .concernsRight instead", "v3.6")
    def isRightMouseButton = isMouseButton(Right)
    /**
     * Whether this event concerns the middle mouse button
     */
    @deprecated("Please use .concernsMiddle instead", "v3.6")
    def isMiddleMouseButton = isMouseButton(Middle)
    
    
    // IMPLEMENTED  ----------------------
    
    override def self = this
    
    override def toString = s"Mouse button $button was ${ if (pressed) "pressed" else "released" } at $position"
    
    override def consumed(event: ConsumeEvent) =
        if (isConsumed) this else copy(consumeEvent = Some(event))
    
    override def withPosition(position: RelativePoint): MouseButtonStateEvent2 = copy(position = position)
    
    
    // OTHER METHODS    ------------------
    
    /**
      * @param button A mouse button
      * @return Whether this event relates to / describes that button
      */
    def concerns(button: MouseButton) = this.button == button
    /**
     * Checks whether this mouse event concerns the specified mouse button
     */
    @deprecated("Please use .concerns(MouseButton) instead", "v3.6")
    def isMouseButton(button: MouseButton) = concerns(button)
}