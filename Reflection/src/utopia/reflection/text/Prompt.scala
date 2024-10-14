package utopia.reflection.text

import utopia.firmament.context.TextContext
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.firmament.localization.LocalizedString

import scala.language.implicitConversions

object Prompt
{
	// IMPLICIT	----------------------------
	
	implicit def wrapLocalizedString(text: LocalizedString)(implicit context: StaticTextContext): Prompt = contextual(text)
	
	implicit def autoLocalizeWithContext(raw: String)(implicit context: StaticTextContext,
													  autoLocalize: String => LocalizedString): Prompt = wrapLocalizedString(raw)
	
	
	// OTHER	----------------------------
	
	/**
	  * Creates a new prompt by utilizing a component creation context
	  * @param text Prompt text
	  * @param context Component creation context (implicit)
	  * @return A new prompt
	  */
	def contextual(text: LocalizedString)(implicit context: StaticTextContext) =
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
