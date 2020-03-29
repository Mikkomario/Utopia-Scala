package utopia.genesis.event

import utopia.genesis.event.MouseButton._

object MouseButtonStatus
{
    /**
      * An empty mouse button status (all buttons are released)
      */
    val empty = MouseButtonStatus(Set())
    
    /**
      * @param buttonIndex Button index that is down
      * @return Mouse button status with specified button down
      */
    def indexDown(buttonIndex: Int) = MouseButtonStatus(Set(buttonIndex))
    
    /**
      * @param button Mouse button that is down
      * @return Mouse button status with specified button down
      */
    def buttonDown(button: MouseButton) = indexDown(button.buttonIndex)
    
    /**
      * @param indices button indices that are down
      * @return Mouse button status with specified buttons down
      */
    def indicesDown(indices: Set[Int]) = MouseButtonStatus(indices)
    
    /**
      * @param buttons Buttons that are down
      * @return Mouse button status with specified buttons down
      */
    def buttonsDown(buttons: Set[MouseButton]) = MouseButtonStatus(buttons.map { _.buttonIndex })
}

/**
 * This immutable data collection save the down state of multiple mouse buttons. This class has value
 * semantics.
 * @author Mikko Hilpinen
 * @since 18.2.2017
 */
case class MouseButtonStatus private(downIndices: Set[Int])
{
    // COMPUTED PROPERTIES    ------
    
    /**
     * Whether the left mouse button is currently being held down
     */
    def isLeftDown = apply(Left)
    
    /**
     * Whether the right mouse button is currently being held down
     */
    def isRightDown = apply(Right)
    
    /**
     * Whether the middle mouse button is currently being held down
     */
    def isMiddleDown = apply(Middle)
    
    /**
      * @return The mouse buttons that are currently considered to be down
      */
    def downButtons = downIndices.flatMap(MouseButton.forIndex)
    
    /**
      * @return The mouse buttons that are currently considered to be up
      */
    def upButtons = MouseButton.values.filterNot { b => downIndices.contains(b.buttonIndex) }
    
    /**
      * @return Copy of this status with left mouse button down
      */
    def withLeftDown = withButtonDown(Left)
    
    /**
      * @return Copy of this status with right mouse button down
      */
    def withRightDown = withButtonDown(Right)
    
    /**
      * @return Copy of this status with middle mouse button down
      */
    def withMiddleDown = withButtonDown(Middle)
    
    /**
      * @return Copy of this status with left mouse button released / up
      */
    def withLeftReleased = withButtonReleased(Left)
    
    /**
      * @return Copy of this status with right mouse button released / up
      */
    def withRightReleased = withButtonReleased(Right)
    
    /**
      * @return Copy of this status with middle mouse button released / up
      */
    def withMiddleReleased = withButtonReleased(Middle)
    
    
    // OPERATORS    ----------------
    
    /**
     * Retrieves the status of a mouse button with the provided button index
     * @param buttonIndex the index of the button
     * @return whether the button is currently pressed down
     */
    def apply(buttonIndex: Int) = downIndices.contains(buttonIndex)
    
    /**
     * Retrieves the status of a mouse button
     * @param button The mouse button the status is for
     * @return whether the button is currently pressed down
     */
    def apply(button: MouseButton): Boolean = apply(button.buttonIndex)
    
    /**
     * Creates a new mouse button status with the specified button status
     * @param newStatus the index of the button and whether the button is currently held down
     * @return new status with the specified status
     */
    def +(buttonIndex: Int, newStatus: Boolean) = withStatus(buttonIndex, newStatus)
    
    /**
     * Creates a new mouse button status with the specified button status
     * @param button The targeted button
     * @param status whether the button is currently held down
     * @return new status with the specified status
     */
    def +(button: MouseButton, status: Boolean) = withStatus(button, status)
    
    /**
      * Combines these statuses in a way that a button is down when it's down in either of these statuses
      * @param other Another mouse button status
      * @return A combined status
      */
    def ||(other: MouseButtonStatus) = MouseButtonStatus(downIndices ++ other.downIndices)
    
    /**
      * Combines these statuses in a way that a button is down when it's down in both of these statuses
      * @param other Another mouse button status
      * @return A combined status
      */
    def &&(other: MouseButtonStatus) = MouseButtonStatus(downIndices.intersect(other.downIndices))
    
    /**
      * @param previous Previous mouse button status
      * @return A status that considers a button to be down only if it was pressed after the previous status
      */
    def pressesSince(previous: MouseButtonStatus) = MouseButtonStatus(downIndices -- previous.downIndices)
    
    /**
      * @param previous Previous mouse button status
      * @return Mouse button indices released since the previous status
      */
    def releasedIndicesSince(previous: MouseButtonStatus) = previous.pressesSince(this).downIndices
    
    /**
      * @param previous Previous mouse button status
      * @return Mouse buttons that were released since the previous status
      */
    def releasedButtonsSince(previous: MouseButtonStatus) = releasedIndicesSince(previous).flatMap(MouseButton.forIndex)
    
    
    // OTHER    --------------------
    
    /**
      * @param buttonIndex Target button index
      * @param status new status for the target button
      * @return A copy of this mouse button status with specified status
      */
    def withStatus(buttonIndex: Int, status: Boolean) =
    {
        if (apply(buttonIndex) == status)
            this
        else if (status)
           MouseButtonStatus(downIndices + buttonIndex)
        else
            MouseButtonStatus(downIndices - buttonIndex)
    }
    
    /**
      * @param button Target button
      * @param status new status for the target button
      * @return A copy of this mouse button status with specified status
      */
    def withStatus(button: MouseButton, status: Boolean): MouseButtonStatus = withStatus(button.buttonIndex, status)
    
    /**
      * @param button Target button
      * @return A copy of this status with specified button down
      */
    def withButtonDown(button: MouseButton) = withStatus(button, true)
    
    /**
      * @param button Target button
      * @return A copy of this status with specified button up / released
      */
    def withButtonReleased(button: MouseButton) = withStatus(button, false)
}