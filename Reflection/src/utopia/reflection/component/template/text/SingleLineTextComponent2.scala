package utopia.reflection.component.template.text

import utopia.reflection.component.template.layout.stack.StackSizeCalculating
import utopia.reflection.shape.stack.{StackLength, StackSize}

/**
  * This is a commom trait for components that present text on a single line
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
trait SingleLineTextComponent2 extends TextComponent2 with StackSizeCalculating
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return Whether this component allows its text to shrink below the standard (desired / specified) size.
	  *         If false, this component will have a minimum stack size specified by text dimensions
	  */
	def allowTextShrink: Boolean
	
	
	// IMPLEMENTED	----------------------
	
	/**
	  * @return The calculated stack size of this component
	  */
	def calculatedStackSize =
	{
		// Adds margins to base text size.
		val insets = this.insets
		val textW = textWidth
		
		val w = (if (allowTextShrink) StackLength.downscaling(textW) else StackLength.fixed(textW)) + insets.horizontal
		val textH = textHeight
		val h = StackLength.fixed(textH) + insets.vertical
		
		StackSize(w, h)
	}
}
