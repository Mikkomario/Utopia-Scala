package utopia.firmament.context

import utopia.firmament.model.stack.LengthExtensions._
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.{StackInsets, StackLength}

object TextContext
{
	// OTHER    -----------------------------
	
	/**
	  * @param base Color context to use as the base settings
	  * @param alignment Text alignment to use (default = left)
	  * @param allowLineBreaks Whether line breaks should be allowed within text components (default = true)
	  * @param allowTextShrink Whether text components should be allowed to shrink their content
	  *                        in order to conserve space (default = true)
	  * @return A new text context instance
	  */
	def apply(base: ColorContext, alignment: Alignment = Alignment.Left, allowLineBreaks: Boolean = true,
	          allowTextShrink: Boolean = true): TextContext =
		_TextContext(base, StackInsets.symmetric(base.margins.small.any, base.margins.verySmall.any),
			alignment, base.margins.verySmall.downscaling, None, allowLineBreaks, allowTextShrink)
	
	
	// NESTED   -----------------------------
	
	private case class _TextContext(wrapped: ColorContext, textInsets: StackInsets, textAlignment: Alignment,
	                                betweenLinesMargin: StackLength,
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
		
		override def withMarginBetweenLines(margin: StackLength): TextContext = copy(betweenLinesMargin = margin)
		
		override def withAllowLineBreaks(allowLineBreaks: Boolean): TextContext =
			if (this.allowLineBreaks == allowLineBreaks) this else copy(allowLineBreaks = allowLineBreaks)
		
		override def withAllowTextShrink(allowTextShrink: Boolean): TextContext =
			if (this.allowTextShrink == allowTextShrink) this else copy(allowTextShrink = allowTextShrink)
		
		override def withColorBase(base: ColorContext): TextContext = if (base == wrapped) this else copy(wrapped = base)
		
		override def *(mod: Double): TextContext = copy(wrapped = wrapped * mod, textInsets = textInsets * mod,
			betweenLinesMargin = betweenLinesMargin * mod, customPromptFont = customPromptFont.map { _ * mod })
	}
}

/**
  * This class specifies a context for components that display text
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
trait TextContext extends TextContextLike[TextContext] with ColorContext