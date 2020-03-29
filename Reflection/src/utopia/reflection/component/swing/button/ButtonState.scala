package utopia.reflection.component.swing.button

import utopia.genesis.color.Color

/**
  * Represents a possible state for a button
  * @author Mikko Hilpinen
  * @since 1.8.2019, v1+
  * @param isEnabled Whether described button is currently enabled
  * @param isInFocus Whether described button is currently in focus
  * @param isMouseOver Whether mouse cursor is currently over the described button
  * @param isPressed Whether the described button is being pressed
  */
case class ButtonState(isEnabled: Boolean, isInFocus: Boolean, isMouseOver: Boolean, isPressed: Boolean)
{
	// IMPLEMENTED	----------------------
	
	override def toString =
	{
		if (!isEnabled)
			"Disabled"
		else if (isPressed)
			"Pressed"
		else if (isMouseOver)
			"Mouse Over"
		else if (isInFocus)
			"In Focus"
		else
			"Default"
	}
	
	
	// OTHER	--------------------------
	
	/**
	  * Modifies provided color to better suit the new button state
	  * @param originalColor Original color
	  * @param intensity Effect intensity modifier. Default = 1.
	  * @return A modified color based on this button state
	  */
	def modify(originalColor: Color, intensity: Double = 1) =
	{
		// TODO: Very light colors may need to be darkened instead
		var c = originalColor
		if (!isEnabled)
			c = c.timesAlpha(0.55)
		if (isMouseOver)
			c = c.lightened(1 + (0.6 * intensity))
		if (isInFocus)
			c = c.lightened(1 + (0.6 * intensity))
		if (isPressed)
			c = c.lightened(1 + (0.6 * intensity))
		
		c
	}
}
