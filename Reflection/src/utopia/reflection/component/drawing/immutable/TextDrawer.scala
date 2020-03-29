package utopia.reflection.component.drawing.immutable

import utopia.genesis.color.Color
import utopia.reflection.component.drawing.template
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.text.Font

object TextDrawer
{
	/**
	  * Creates a new immutable text drawer
	  * @param text Text to draw
	  * @param font Font used when drawing the text
	  * @param color Color used when drawing the text (default = black)
	  * @param alignment Alignment used when drawing the text (default = left)
	  * @param insets Insets placed around the text when possible (default = 0 on each side)
	  * @param drawLevel Draw level used (default = Normal)
	  * @return A new text drawer
	  */
	def apply(text: LocalizedString, font: Font, color: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
			  insets: StackInsets = StackInsets.any, drawLevel: DrawLevel = Normal) =
		new TextDrawer(text, TextDrawContext(font, color, alignment, insets), drawLevel)
}

/**
  * A custom drawer used for drawing static text with static settings
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
class TextDrawer(override val text: LocalizedString, override val drawContext: TextDrawContext,
				 override val drawLevel: DrawLevel = Normal) extends template.TextDrawer
