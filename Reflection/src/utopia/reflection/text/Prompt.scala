package utopia.reflection.text

import scala.language.implicitConversions

import utopia.genesis.color.Color
import utopia.reflection.localization.LocalizedString
import utopia.reflection.util.ComponentContext

object Prompt
{
	// IMPLICIT	----------------------------
	
	implicit def wrapLocalizedString(text: LocalizedString)(implicit context: ComponentContext): Prompt = contextual(text)
	
	implicit def autoLocalizeWithContext(raw: String)(implicit context: ComponentContext,
													  autoLocalize: String => LocalizedString): Prompt = wrapLocalizedString(raw)
	
	
	// OTHER	----------------------------
	
	/**
	  * Creates a new prompt by utilizing a component creation context
	  * @param text Prompt text
	  * @param context Component creation context (implicit)
	  * @return A new prompt
	  */
	def contextual(text: LocalizedString)(implicit context: ComponentContext) =
		Prompt(text, context.promptFont, context.promptTextColor)
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
