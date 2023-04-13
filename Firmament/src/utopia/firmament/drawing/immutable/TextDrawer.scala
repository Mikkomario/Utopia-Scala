package utopia.firmament.drawing.immutable

import utopia.genesis.graphics.MeasuredText
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.template.{DrawLevel, TextDrawerLike}
import utopia.firmament.model.stack.StackInsets

/**
  * A custom drawer used for drawing static text with static settings
  * @author Mikko Hilpinen
  * @since 14.3.2020, Reflection v1
  */
case class TextDrawer(override val text: MeasuredText, override val font: Font,
                      override val insets: StackInsets = StackInsets.any, override val color: Color = Color.textBlack,
                      override val alignment: Alignment = Alignment.Left, override val drawLevel: DrawLevel = Normal)
	extends TextDrawerLike
