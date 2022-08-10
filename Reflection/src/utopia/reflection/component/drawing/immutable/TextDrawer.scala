package utopia.reflection.component.drawing.immutable

import utopia.paradigm.color.Color
import utopia.reflection.component.drawing.template.{DrawLevel, TextDrawerLike}
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.localization.LocalizedString
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.StackInsets
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
	  * @param allowMultipleLines Whether line splitting should be allowed when drawing (based on line breaks)
	  *                           (default = true)
	  * @return A new text drawer
	  */
	def apply(text: LocalizedString, font: Font, color: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
			  insets: StackInsets = StackInsets.any, drawLevel: DrawLevel = Normal, allowMultipleLines: Boolean = true) =
		new TextDrawer(text, TextDrawContext(font, color, alignment, insets), drawLevel, allowMultipleLines)
}

/**
  * A custom drawer used for drawing static text with static settings
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
case class TextDrawer(text: LocalizedString, override val drawContext: TextDrawContext,
					  override val drawLevel: DrawLevel, allowMultipleLines: Boolean)
	extends TextDrawerLike
{
	override def drawnText =
		if (allowMultipleLines) Right(text.lines) else Left(text)
}
