package utopia.genesis.handling.event.mouse

import utopia.genesis.event.MouseButton
import utopia.genesis.event.MouseButton._

object MouseButtonStates
{
	// ATTRIBUTES   -------------------
	
	/**
	  * The default set of mouse button states, where all buttons are released
	  */
	val default: MouseButtonStates = apply(Set[MouseButton]())
	
	
	// OTHER    -----------------------
	
	/**
	  * @param button A mouse button that is pressed
	  * @return Button states where that button is pressed
	  */
	def apply(button: MouseButton): MouseButtonStates = apply(Set(button))
	/**
	  * @param button1 A mouse button that is pressed
	  * @param button2 Another mouse button that is pressed
	  * @param more Other mouse buttons that are pressed
	  * @return Button states where that button is pressed
	  */
	def apply(button1: MouseButton, button2: MouseButton, more: MouseButton*): MouseButtonStates =
		apply(Set(button1, button2) ++ more)
}

/**
 * Represents a snapshot of a mouse's button status, recording the buttons that were held down during the capture time.
 * @author Mikko Hilpinen
 * @since 18.2.2017, refactored 5.2.2024, v4.0
 */
case class MouseButtonStates(buttonsPressed: Set[MouseButton])
{
	// ATTRIBUTES   ----------
	
	/**
	  * Mouse button -indices for the buttons which are currently pressed
	  */
	lazy val downIndices = buttonsPressed.map { _.index }
	
	
    // COMPUTED --------------
	
	/**
	  * @return Whether all the mouse buttons are in the released state
	  */
	def areAllReleased = buttonsPressed.isEmpty
	/**
	  * @return Whether there's at least one mouse button pressed at the moment
	  */
	def areSomePressed = !areAllReleased
	
    /**
      * @return Whether this status is empty (no mouse button is currently down)
      */
    @deprecated("Please use .areAllReleased instead", "v4.0")
    def isEmpty = areAllReleased
    /**
      * @return Whether one or more mouse buttons are currently held down
      */
    @deprecated("Please use .areSomePressed instead", "v4.0")
    def isAnyButtonPressed = areSomePressed
	
	/**
	  * @return Whether the left mouse button is currently pressed
	  */
	def left = apply(Left)
	/**
	  * @return Whether the right mouse button is currently pressed
	  */
	def right = apply(Right)
	/**
	  * @return Whether the middle mouse button is currently pressed
	  */
	def middle = apply(Middle)
	
    /**
     * Whether the left mouse button is currently being held down
     */
    @deprecated("Please use .left instead", "v4.0")
    def isLeftDown = apply(Left)
    /**
     * Whether the right mouse button is currently being held down
     */
    @deprecated("Please use .right instead", "v4.0")
    def isRightDown = apply(Right)
    /**
     * Whether the middle mouse button is currently being held down
     */
    @deprecated("Please use .middle instead", "v4.0")
    def isMiddleDown = apply(Middle)
    
    /**
      * @return The mouse buttons that are currently considered to be down
      */
    @deprecated("Please use .buttonsPressed instead", "v4.0")
    def downButtons = buttonsPressed
    /**
      * @return The mouse buttons that are currently considered to be up
      */
    @deprecated("Deprecated for removal", "v4.0")
    def upButtons = MouseButton.standardValues.toSet -- buttonsPressed
    
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
    def apply(button: MouseButton): Boolean = apply(button.index)
	
	/**
	  * @param button A button that is pressed
	  * @return Copy of this set with the specified state
	  */
	def +(button: MouseButton) = withButtonDown(button)
	/**
	  * @param buttons Buttons that are pressed
	  * @return Copy of this set with the specified states
	  */
	def ++(buttons: IterableOnce[MouseButton]) = copy(buttonsPressed = buttonsPressed ++ buttons)
	/**
	  * @param button A button that is released
	  * @return Copy of this set with the specified state
	  */
	def -(button: MouseButton) = withButtonReleased(button)
	/**
	  * @param buttons Buttons that are released
	  * @return Copy of this set with the specified states
	  */
	def --(buttons: IterableOnce[MouseButton]) = copy(buttonsPressed = buttonsPressed -- buttons)
    
    /**
      * Combines these statuses in a way that a button is down when it's down in either of these statuses
      * @param other Another mouse button status
      * @return A combined status
      */
    def ||(other: MouseButtonStates) = copy(buttonsPressed = buttonsPressed ++ other.buttonsPressed)
    /**
      * Combines these statuses in a way that a button is down when it's down in both of these statuses
      * @param other Another mouse button status
      * @return A combined status
      */
    def &&(other: MouseButtonStates) = copy(buttonsPressed = buttonsPressed.intersect(other.buttonsPressed))
    
    /**
      * @param previous Previous mouse button status
      * @return A status that considers a button to be down only if it was pressed after the previous status
      */
    def pressesSince(previous: MouseButtonStates) = copy(buttonsPressed = buttonsPressed -- previous.buttonsPressed)
	/**
	  * @param previous A previous set of mouse button states
	  * @return Buttons that were released between these states
	  */
	def buttonsReleasedSince(previous: MouseButtonStates) = previous.pressesSince(this).buttonsPressed
    /**
      * @param previous Previous mouse button status
      * @return Mouse button indices released since the previous status
      */
    @deprecated("Please use .buttonsReleasedSince(MouseButtonStates) instead", "v4.0")
    def releasedIndicesSince(previous: MouseButtonStates) = previous.pressesSince(this).downIndices
    /**
      * @param previous Previous mouse button status
      * @return Mouse buttons that were released since the previous status
      */
    @deprecated("Please use .buttonsReleasedSince(MouseButtonState) instead", "v4.0")
    def releasedButtonsSince(previous: MouseButtonStates) = buttonsReleasedSince(previous)
    
    
    // OTHER    --------------------
	
	/**
	  * @param button A mouse button
	  * @param pressed Whether that button is pressed (true) or released (false)
	  * @return Copy of this set with the specified button state
	  */
	def withButtonState(button: MouseButton, pressed: Boolean) = {
		if (apply(button) == pressed)
			this
		else if (pressed)
			copy(buttonsPressed = buttonsPressed + button)
		else
			copy(buttonsPressed = buttonsPressed - button)
	}
    /**
      * @param buttonIndex Target button index
      * @param status new status for the target button
      * @return A copy of this mouse button status with specified status
      */
    @deprecated("Please use .withButtonState(MouseButton, Boolean) instead", "v4.0")
    def withStatus(buttonIndex: Int, status: Boolean) =
	    withButtonState(MouseButton(buttonIndex), status)
	/**
	  * @param button Target button
	  * @param status new status for the target button
	  * @return A copy of this mouse button status with specified status
	  */
	@deprecated("Please use .withButtonState(MouseButton, Boolean) instead", "v4.0")
	def withStatus(button: MouseButton, status: Boolean): MouseButtonStates = withButtonState(button, status)
    /**
      * @param button Target button
      * @return A copy of this status with specified button down
      */
    def withButtonDown(button: MouseButton) = withButtonState(button, pressed = true)
    /**
      * @param button Target button
      * @return A copy of this status with specified button up / released
      */
    def withButtonReleased(button: MouseButton) = withButtonState(button, pressed = false)
}