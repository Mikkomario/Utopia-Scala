package utopia.reflection.component.drawing.immutable

import utopia.genesis.color.Color
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

/**
  * Context required when drawing text
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  * @param font Font used when drawing the text
  * @param color Color used when drawing the text
  * @param alignment Alignment used for positioning the text within the target context
  * @param insets Insets placed around the text within the target context
  * @param betweenLinesMargin Vertical margin placed between each line of text (default = 0.0)
  */
case class TextDrawContext(font: Font, color: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
						   insets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0)
{
	// OTHER	------------------------------------
	
	/**
	  * @param other Another text draw context
	  * @return Whether this context has different size-affecting qualities than the other context
	  */
	def hasSameDimensionsAs(other: TextDrawContext) = font == other.font && insets == other.insets &&
		betweenLinesMargin == other.betweenLinesMargin
	
	/**
	  * @param f A mapping function for font
	  * @return A copy of this context with mapped font
	  */
	def mapFont(f: Font => Font) = copy(font = f(font))
	
	/**
	  * @param f A mapping function for text color
	  * @return A copy of this context with mapped text color
	  */
	def mapColor(f: Color => Color) = copy(color = f(color))
}
