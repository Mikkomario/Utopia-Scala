package utopia.reflection.component.drawing.immutable

import utopia.genesis.color.Color
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.template.{DrawLevel, TextDrawerLike2}
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.{Font, MeasuredText}

/**
  * A custom drawer used for drawing static text with static settings
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
case class TextDrawer2(override val text: MeasuredText, override val font: Font,
					   override val insets: StackInsets = StackInsets.any, override val color: Color = Color.textBlack,
					   override val drawLevel: DrawLevel = Normal)
	extends TextDrawerLike2
