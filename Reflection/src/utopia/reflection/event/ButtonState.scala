package utopia.reflection.event

import utopia.paradigm.color.Color

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
	// COMPUTED --------------------------
	
	/**
	 * @return A hover effect alpha level that should be used for this state
	 */
	def hoverAlpha =
	{
		if (!isEnabled)
			0.0
		else if (isPressed)
			0.25
		else if (isInFocus)
			0.2
		else if (isMouseOver)
			0.15
		else
			0.0
	}
	
	
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
		val modLevel = Vector(isMouseOver, isInFocus, isPressed).count { b => b }
		val base = if (modLevel == 0) originalColor else originalColor.highlightedBy(modLevel)
		if (isEnabled)
			base
		else
			base.timesAlpha(0.55)
	}
}
