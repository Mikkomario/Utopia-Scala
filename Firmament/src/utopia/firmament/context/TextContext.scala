package utopia.firmament.context

import utopia.firmament.model.stack.LengthExtensions._
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.{StackInsets, StackLength}

import scala.language.implicitConversions

@deprecated("Replaced with a new version", "v1.4")
object TextContext
{
	// IMPLICIT -----------------------------
	
	// Implicitly wraps a ColorContext into a TextContext
	implicit def wrap(base: ColorContext): TextContext = apply(base)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param base Color context to use as the base settings
	  * @param alignment Text alignment to use (default = left)
	  * @param lineSplitThreshold A width threshold after which lines should be split.
	  *                           None if no automated line-splitting should occur (default).
	  * @param allowLineBreaks Whether line breaks should be allowed within text components (default = true)
	  * @param allowTextShrink Whether text components should be allowed to shrink their content
	  *                        in order to conserve space (default = false)
	  * @return A new text context instance
	  */
	def apply(base: ColorContext, alignment: Alignment = Alignment.Left, lineSplitThreshold: Option[Double] = None,
	          allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false): TextContext =
		_TextContext(base, StackInsets.symmetric(base.margins.aroundSmall, base.margins.aroundVerySmall),
			alignment, lineSplitThreshold, base.margins.verySmall.downscaling, None, allowLineBreaks, allowTextShrink)
	
	
	// NESTED   -----------------------------
	
	private case class _TextContext(colorContext: ColorContext, textInsets: StackInsets, textAlignment: Alignment,
	                                lineSplitThreshold: Option[Double], betweenLinesMargin: StackLength,
	                                customPromptFont: Option[Font], allowLineBreaks: Boolean, allowTextShrink: Boolean)
		extends TextContext
	{
		override def self: TextContext = this
		
		override def promptFont: Font = customPromptFont.getOrElse(font)
		
		override def withDefaultPromptFont: TextContext =
			if (customPromptFont.isEmpty) this else copy(customPromptFont = None)
		override def withPromptFont(font: Font): TextContext = copy(customPromptFont = Some(font))
		
		override def withTextAlignment(alignment: Alignment): TextContext =
			if (alignment == textAlignment) this else copy(textAlignment = alignment)
		
		override def withTextInsets(insets: StackInsets): TextContext = copy(textInsets = insets)
		
		override def withLineSplitThreshold(threshold: Option[Double]): TextContext =
			copy(lineSplitThreshold = threshold)
		override def withMarginBetweenLines(margin: StackLength): TextContext = copy(betweenLinesMargin = margin)
		
		override def withAllowLineBreaks(allowLineBreaks: Boolean): TextContext =
			if (this.allowLineBreaks == allowLineBreaks) this else copy(allowLineBreaks = allowLineBreaks)
		
		override def withAllowTextShrink(allowTextShrink: Boolean): TextContext =
			if (this.allowTextShrink == allowTextShrink) this else copy(allowTextShrink = allowTextShrink)
		
		override def withColorContext(base: ColorContext): TextContext =
			if (base == colorContext) this else copy(colorContext = base)
		
		override def *(mod: Double): TextContext = copy(colorContext = colorContext * mod, textInsets = textInsets * mod,
			betweenLinesMargin = betweenLinesMargin * mod, customPromptFont = customPromptFont.map { _ * mod })
	}
}

/**
  * This class specifies a context for components that display text
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
@deprecated("Replaced with a new version", "v1.4")
trait TextContext extends TextContextLike[TextContext] with ColorContext