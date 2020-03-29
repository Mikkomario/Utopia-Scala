package utopia.reflection.component

import utopia.reflection.component.stack.StackSizeCalculating
import utopia.reflection.shape.{StackLength, StackSize}

/**
  * This is a commom trait for components that present text on a single line
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
trait SingleLineTextComponent extends TextComponent with StackSizeCalculating
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return Whether this component has a minimum width based on text size. If false, text may not always show.
	  */
	def hasMinWidth: Boolean
	
	
	// IMPLEMENTED	----------------------
	
	/**
	  * @return The calculated stack size of this component
	  */
	def calculatedStackSize =
	{
		// Adds margins to base text size.
		val insets = this.insets
		val textW = textWidth.getOrElse(128)
		
		val w = (if (hasMinWidth) StackLength.fixed(textW) else StackLength.downscaling(textW)) + insets.horizontal
		val textH = textHeight.getOrElse(32)
		val h = StackLength.fixed(textH) + insets.vertical
		
		StackSize(w, h)
	}
}
