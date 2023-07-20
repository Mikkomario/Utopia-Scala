package utopia.firmament.model

import utopia.firmament.context.TextContext
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.StackInsets

object TextDrawContext
{
	/**
	  * Creates a new text draw context utilizing a component creation context
	  * @param context The component creation context to use (implicit)
	  * @return A new text draw context with settings from the component creation context
	  */
	def contextual(implicit context: TextContext) = createContextual()
	def contextualHint(implicit context: TextContext) = createContextual(isHint = true)
	
	/**
	  * Creates a new text draw context utilizing a component creation context
	  * @param isHint Whether tyling is modified for hint text
	  * @param context The component creation context to use (implicit)
	  * @return A new text draw context with settings from the component creation context
	  */
	def createContextual(isHint: Boolean = false)(implicit context: TextContext) =
		TextDrawContext(context.font, if (isHint) context.hintTextColor else context.textColor, context.textAlignment,
			context.textInsets, context.lineSplitThreshold, context.betweenLinesMargin.optimal, context.allowLineBreaks)
}

/**
  * Context required when drawing text
  * @author Mikko Hilpinen
  * @since 14.3.2020, Reflection v1
  * @param font Font used when drawing the text
  * @param color Color used when drawing the text
  * @param alignment Alignment used for positioning the text within the target context
  * @param insets Insets placed around the text within the target context
  * @param lineSplitThreshold A width threshold at which a new line is started.
  *                           None if there should not be any automatic line-splitting (default).
  * @param betweenLinesMargin Vertical margin placed between each line of text (default = 0.0)
  * @param allowLineBreaks Whether text line splitting should be allowed (default = false)
  */
case class TextDrawContext(font: Font, color: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
						   insets: StackInsets = StackInsets.any, lineSplitThreshold: Option[Double] = None,
						   betweenLinesMargin: Double = 0.0, allowLineBreaks: Boolean = false)
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
	  * @param allow Whether use of line breaks should be allowed
	  * @return A copy of this context with the specified setting
	  */
	def withAllowLineBreaks(allow: Boolean) = {
		if (allow == allowLineBreaks)
			this
		else
			copy(allowLineBreaks = allow)
	}
	
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
