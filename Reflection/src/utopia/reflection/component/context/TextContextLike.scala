package utopia.reflection.component.context

import utopia.firmament.model.TextDrawContext
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.firmament.localization.Localizer
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.{StackInsets, StackLength}

/**
  * A common trait for text context implementations
  * @author Mikko
  * @since 27.4.2020, v
  */
@deprecated("Moved to Firmament", "v2.0")
trait TextContextLike extends ColorContextLike
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The localizer used in this context
	  */
	def localizer: Localizer
	
	/**
	  * @return The font used in texts
	  */
	def font: Font
	
	/**
	  * @return The font used in prompts
	  */
	def promptFont: Font
	
	/**
	  * @return Text alignment used
	  */
	def textAlignment: Alignment
	
	/**
	  * @return Insets / margins placed around drawn text
	  */
	def textInsets: StackInsets
	
	/**
	  * @return Margin placed between lines of text
	  */
	def betweenLinesMargin: StackLength
	
	/**
	  * @return Color used in the text
	  */
	def textColor: Color
	
	/**
	  * @return Whether text display components should by default respect line breaks inside the displayed text,
	  *         drawing possibly multiple separate lines of text. If false, components should, by default, ignore
	  *         line breaks.
	  */
	def allowLineBreaks: Boolean
	
	/**
	  * @return Whether displayed text should be shrank to conserve space when that seems necessary
	  */
	def allowTextShrink: Boolean
	
	
	// COMPUTED	---------------------------
	
	/**
	  * @return Whether displayed text components should always have a minimum width so that the text is not cut off
	  */
	@deprecated("Replaced with allowTextShrink", "v2")
	def textHasMinWidth = !allowTextShrink
	
	/**
	 * @return The text draw context defined by this context
	 */
	def textDrawContext = TextDrawContext(font, textColor, textAlignment, textInsets, betweenLinesMargin.optimal)
	
	
	// OTHER	--------------------------
	
	/**
	  * @return Color used in hint texts and disabled elements
	  */
	def hintTextColor = containerBackground.textColorStandard.hintTextColor
}
