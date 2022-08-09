package utopia.reflection.text

import utopia.paradigm.color.Color
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.localization.LocalizedString

sealed trait RichText
{
	// TODO: Work in progress
}

object RichTextElement
{
	/**
	  * Creates a new rich text element utilizing text draw context
	  * @param text Text to draw
	  * @param hasBackground Whether a solid background should be drawn behind the text (default = false)
	  * @param isHint Whether this text is a hint (default = false)
	  * @param context Text context (implicit)
	  * @return New rich text instance
	  */
	def contextual(text: LocalizedString, hasBackground: Boolean = false, isHint: Boolean = false)
	              (implicit context: TextContextLike) =
	{
		val background = if (hasBackground) Some(context.containerBackground.background) else None
		if (isHint)
			apply(text, context.promptFont, context.hintTextColor, background)
		else
			apply(text, context.font, context.textColor, background)
	}
}

/**
  * Used for drawing text with alternating fonts, colors and background
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.0
  */
case class RichTextElement(text: LocalizedString, font: Font, color: Color = Color.textBlack,
                           background: Option[Color] = None) extends RichText

/**
  * A combination of multiple sequential rich text elements
  * @param parts Rich text element parts that form this combination
  */
case class CombinedRichText(parts: Vector[RichTextElement]) extends RichText
