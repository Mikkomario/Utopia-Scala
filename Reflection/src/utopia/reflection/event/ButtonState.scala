package utopia.reflection.event

import utopia.genesis.color.Color

object ButtonState
{
	/**
	  * The default button state
	  */
	val default = ButtonState(isEnabled = true, isInFocus = false, isMouseOver = false, isPressed = false)
}

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
		// Either lightens or darkens the colors
		def mod(color: Color) =
		{
			val luminosityMod = color.luminosity
			if (originalColor.luminosity >= 0.6)
				color.darkened(1 + (1 - luminosityMod) * intensity * 0.3 + 0.1)
			else
				color.lightened(1 + luminosityMod * intensity * 0.7 + 0.1)
		}
		
		var c = originalColor
		if (!isEnabled)
			c = c.timesAlpha(0.55)
		if (isMouseOver)
			c = mod(c)
		if (isInFocus)
			c = mod(c)
		if (isPressed)
			c = mod(c)
		
		c
	}
}
