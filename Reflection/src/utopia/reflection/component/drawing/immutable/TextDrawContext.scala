package utopia.reflection.component.drawing.immutable

import utopia.paradigm.color.Color
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object TextDrawContext
{
	/**
	  * Creates a new text draw context utilizing a component creation context
	  * @param context The component creation context to use (implicit)
	  * @return A new text draw context with settings from the component creation context
	  */
	def contextual(implicit context: TextContextLike) = TextDrawContext(context.font, context.textColor,
		context.textAlignment, context.textInsets, context.betweenLinesMargin.optimal)
}

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
	// COMPUTED	------------------------------------
	
	/**
	  * @return A copy of this context that expands the horizontal text insets to a direction most suitable for the
	  *         currently selected text alignment
	  */
	def expandingHorizontally = mapInsets { _.expandingHorizontallyAccordingTo(alignment) }
	
	
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
	
	/**
	  * @param f A mapping function for text insets
	  * @return A copy of this context with mapped text insets
	  */
	def mapInsets(f: StackInsets => StackInsets) = copy(insets = f(insets))
}
