package utopia.reflection.component.template.text

import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.template.ComponentLike2
import utopia.reflection.component.template.layout.stack.StackSizeCalculating
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.text.MeasuredText

/**
  * Common trait for components that present text
  * @author Mikko Hilpinen
  * @since 10.12.2019, v1
  */
trait TextComponent2 extends ComponentLike2 with StackSizeCalculating
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The text currently presented in this component
	  */
	def measuredText: MeasuredText
	
	/**
	  * @return Context for drawing the text within this component
	  */
	def drawContext: TextDrawContext
	
	/**
	  * @return Whether this component allows its text to shrink below the standard (desired / specified) size.
	  *         If false, this component will have a minimum stack size specified by text dimensions
	  */
	def allowTextShrink: Boolean
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Text displayed in this component
	  */
	def text = measuredText.text
	
	/**
	  * @return The insets around the text in this component
	  */
	def insets = drawContext.insets
	
	/**
	  * @return This component's text alignment
	  */
	def alignment = drawContext.alignment
	
	/**
	  * @return The font used in this component
	  */
	def font = drawContext.font
	
	/**
	  * @return The color of the text in this component
	  */
	def textColor = drawContext.color
	
	
	// IMPLEMENTED	-----------------------
	
	/**
	  * @return The calculated stack size of this component
	  */
	def calculatedStackSize =
	{
		// Adds margins to base text size.
		val insets = this.insets
		val textSize = measuredText.size
		
		if (allowTextShrink)
			StackSize.downscaling(textSize) + insets
		else
			StackSize.fixed(textSize) + insets
	}
}
