package utopia.firmament.context.text

import utopia.firmament.context.color.{StaticColorContext, StaticColorContextWrapper}
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.firmament.model.stack.LengthExtensions._
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.Alignment

import scala.language.implicitConversions

object StaticTextContext
{
	// IMPLICIT ---------------------------
	
	implicit def wrap(c: StaticColorContext): StaticTextContext = apply(c)
	
	
	// OTHER    ---------------------------
	
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
	def apply(base: StaticColorContext, alignment: Alignment = Alignment.Left,
	          lineSplitThreshold: Option[Double] = None,
	          allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false): StaticTextContext =
		_TextContext(base, StackInsets.symmetric(base.margins.aroundSmall, base.margins.aroundVerySmall),
			alignment, lineSplitThreshold, base.margins.verySmall.downscaling, None, allowLineBreaks, allowTextShrink)
			
	
	// NESTED   ---------------------------
	
	private case class _TextContext(base: StaticColorContext, textInsets: StackInsets, textAlignment: Alignment,
	                                lineSplitThreshold: Option[Double], betweenLinesMargin: StackLength,
	                                customPromptFont: Option[Font], allowLineBreaks: Boolean, allowTextShrink: Boolean)
		extends StaticColorContextWrapper[StaticColorContext, StaticTextContext] with StaticTextContext
	{
		// ATTRIBUTES   ----------------------------
		
		override lazy val textDrawContext = super.textDrawContext
		
		
		// IMPLEMENTED  ----------------------------
		
		override def self = this
		
		override def promptFont: Font = customPromptFont.getOrElse(font)
		
		override def withDefaultPromptFont =
			if (customPromptFont.isEmpty) this else copy(customPromptFont = None)
		
		override def withBase(base: StaticColorContext): StaticTextContext = copy(base = base)
		
		override def withPromptFont(font: Font) = copy(customPromptFont = Some(font))
		override def withTextAlignment(alignment: Alignment) =
			if (alignment == textAlignment) this else copy(textAlignment = alignment)
		override def withTextInsets(insets: StackInsets) = copy(textInsets = insets)
		override def withLineSplitThreshold(threshold: Option[Double]) =
			copy(lineSplitThreshold = threshold)
		override def withMarginBetweenLines(margin: StackLength) = copy(betweenLinesMargin = margin)
		override def withAllowLineBreaks(allowLineBreaks: Boolean) =
			if (this.allowLineBreaks == allowLineBreaks) this else copy(allowLineBreaks = allowLineBreaks)
		override def withAllowTextShrink(allowTextShrink: Boolean) =
			if (this.allowTextShrink == allowTextShrink) this else copy(allowTextShrink = allowTextShrink)
		
		override def *(mod: Double) = copy(base = base * mod, textInsets = textInsets * mod,
			betweenLinesMargin = betweenLinesMargin * mod, customPromptFont = customPromptFont.map { _ * mod })
	}
}

/**
  * Common trait for static [[TextContext2]] implementations.
  * Removes generic type parameter from [[StaticTextContextLike]].
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.3.2
  */
trait StaticTextContext extends StaticColorContext with StaticTextContextLike[StaticTextContext]
