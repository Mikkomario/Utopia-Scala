package utopia.firmament.component.text

import utopia.firmament.component.Component
import utopia.firmament.component.stack.StackSizeCalculating
import utopia.firmament.model.TextDrawContext
import utopia.genesis.graphics.MeasuredText
import utopia.firmament.localization.LocalizedString
import utopia.reflection.shape.stack.StackSize

/**
  * Common trait for components that present text
  * @author Mikko Hilpinen
  * @since 10.12.2019, Reflection v1
  */
trait TextComponent extends Component with HasTextDrawContext with StackSizeCalculating
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The text currently presented in this component
	  */
	def measuredText: MeasuredText
	
	/**
	  * @return Whether this component allows its text to shrink below the standard (desired / specified) size.
	  *         If false, this component will have a minimum stack size specified by text dimensions
	  */
	def allowTextShrink: Boolean
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Font metrics used in this component (with the current font)
	  */
	def fontMetrics = fontMetricsWith(font)
	
	
	// IMPLEMENTED	-----------------------
	
	/**
	  * @return The calculated stack size of this component
	  */
	def calculatedStackSize = calculatedStackSizeWith(measuredText)
	
	
	// OTHER	--------------------------
	
	/**
	  * @param text Text to measure
	  * @param style Style to use when measuring text (default = current component style)
	  * @return A measured copy of that text within this component
	  */
	def measure(text: LocalizedString, style: TextDrawContext = textDrawContext) = {
		MeasuredText(text.string, fontMetricsWith(style.font),
			betweenLinesAdjustment = style.betweenLinesMargin, allowLineBreaks = style.allowLineBreaks)
	}
	
	/**
	  * @param text Pre-measured text
	  * @return Calculated stack size of this component when containing specified text
	  */
	def calculatedStackSizeWith(text: MeasuredText) = {
		// Adds margins to base text size.
		val insets = this.textInsets
		val textSize = text.size
		
		if (allowTextShrink)
			StackSize.downscaling(textSize) + insets
		else
			StackSize.fixed(textSize) + insets
	}
	
	/**
	  * @param text Text to measure and use
	  * @return Calculated stack size of this component when containing specified text
	  */
	def calculatedStackSizeWith(text: LocalizedString): StackSize = calculatedStackSizeWith(measure(text))
}
