package utopia.genesis.handling.event.mouse

import utopia.genesis.event.MouseButton._
import utopia.genesis.event.{Consumable, ConsumeEvent, MouseButton}
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

/**
 * Common traits for mouse events concerning a mouse button state change
  * (i.e. a mouse button press or a mouse button release)
 * @author Mikko Hilpinen
 * @since 6.2.2024, v3.6
 */
trait MouseButtonStateEventLike[+Repr] extends MouseEvent2[Repr]
{
    // ABSTRACT --------------------------
    
    /**
      * @return The mouse button associated with this event (the one that was pressed or released)
      */
    def button: MouseButton
    
    /**
      * @return Whether the mouse button was pressed (true) or released (false)
      */
    def pressed: Boolean
    
    
    // COMPUTED PROPERTIES    ------------
    
    /**
      * @return Whether the mouse button was released / whether this event was triggered by a mouse button release
      */
    def released = !pressed
    
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
    
    
    // OTHER METHODS    ------------------
    
    /**
      * @param button A mouse button
      * @return Whether this event relates to / describes that button
      */
    def concerns(button: MouseButton) = this.button == button
}