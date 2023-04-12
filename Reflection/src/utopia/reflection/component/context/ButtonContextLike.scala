package utopia.reflection.component.context

import utopia.reflection.color.ComponentColor

/**
  * A common trait for button specification contexts
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
@deprecated("Deprecated for removal", "v2.0")
trait ButtonContextLike extends TextContextLike
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return Color of the created buttons
	  */
	def buttonColor: ComponentColor
	
	/**
	  * @return Border width specification for the buttons
	  */
	def borderWidth: Double
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return A highlighted version of the button color, suitable for focus highlight etc.
	  */
	def buttonColorHighlighted = buttonColor.highlighted
	
	
	// IMPLEMENTED	------------------------
	
	override def textColor = buttonColor.defaultTextColor
}
