package utopia.reflection.text

import scala.language.implicitConversions
import utopia.genesis.color.Color
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.localization.LocalizedString

object Prompt
{
	// IMPLICIT	----------------------------
	
	implicit def wrapLocalizedString(text: LocalizedString)(implicit context: TextContextLike): Prompt = contextual(text)
	
	implicit def autoLocalizeWithContext(raw: String)(implicit context: TextContextLike,
													  autoLocalize: String => LocalizedString): Prompt = wrapLocalizedString(raw)
	
	
	// OTHER	----------------------------
	
	/**
	  * Creates a new prompt by utilizing a component creation context
	  * @param text Prompt text
	  * @param context Component creation context (implicit)
	  * @return A new prompt
	  */
	def contextual(text: LocalizedString)(implicit context: TextContextLike) =
		Prompt(text, context.promptFont, context.hintTextColor)
}

/**
  * Prompts are used for hinting input values
  * @author Mikko Hilpinen
  * @since 1.5.2019, v1+
  * @param text The text displayed in the prompt
  * @param font The font used
  * @param color the font color (default = 55% opaque black)
  */
case class Prompt(text: LocalizedString, font: Font, color: Color = Color.textBlackDisabled)
