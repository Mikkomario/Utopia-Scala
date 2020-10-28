package utopia.reflection.component.template.text

import utopia.genesis.shape.shape2D.Size
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.template.ComponentLike2
import utopia.reflection.component.template.layout.stack.StackSizeCalculating
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.stack.StackSize

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
	def text: LocalizedString
	
	/**
	  * @return Context for drawing the text within this component
	  */
	def drawContext: TextDrawContext
	
	/**
	  * @return Whether this component allows its text to shrink below the standard (desired / specified) size.
	  *         If false, this component will have a minimum stack size specified by text dimensions
	  */
	def allowTextShrink: Boolean
	
	/**
	  * @return Whether line breaks in the text should be respected
	  */
	def allowLineBreaks: Boolean
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return The individual text lines within this component's text
	  */
	def lines = text.lines
	
	/**
	  * @return The width of the current text in this component
	  */
	def singleLineTextWidth = textWidthWith(text.string)
	
	/**
	  * @return The height of the current text in this component
	  */
	def singleLineTextHeight = textHeightWith(font)
	
	/**
	  * @return The size of the current text inside this component
	  */
	def textSize = textSizeWith(text.string, allowLineBreaks)
	
	/**
	  * @return The size of the current text in this component when no line breaks are considered
	  */
	def singleLineTextSize = textSizeWith(text.string, recognizeLineBreaks = false)
	
	/**
	  * @return The insets around the text in this component
	  */
	def insets = drawContext.insets
	
	/**
	  * @return The margin placed between lines of text, in cases where line breaks are used and allowed
	  */
	def betweenLinesMargin = drawContext.betweenLinesMargin
	
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
		val textSize = this.textSize
		
		if (allowTextShrink)
			StackSize.downscaling(textSize) + insets
		else
			StackSize.fixed(textSize) + insets
	}
	
	
	// OTHER	---------------------------
	
	/**
	  * @param text Text
	  * @return Width of that text inside this component
	  */
	def textWidthWith(text: String): Int = textWidthWith(font, text)
	
	/**
	  * Calculates the total text size using the specified string
	  * @param text Text to measure
	  * @param recognizeLineBreaks Whether line breaks should be considered (true) or whether the text should be
	  *                            forced / considered single line only (false). Default = true.
	  * @return Measurements the text would have in this component
	  */
	def textSizeWith(text: String, recognizeLineBreaks: Boolean = true) =
	{
		if (recognizeLineBreaks)
		{
			val lines = text.linesIterator.toVector
			if (lines.isEmpty)
				Size.zero
			else
			{
				val maxWidth = lines.map(textWidthWith).max
				val numberOfLines = lines.size
				val totalMargin = if (numberOfLines > 1) (numberOfLines - 1) * betweenLinesMargin else 0.0
				val totalHeight = numberOfLines * singleLineTextHeight + totalMargin
				Size(maxWidth, totalHeight)
			}
		}
		else
			Size(textWidthWith(text), singleLineTextHeight)
	}
}
