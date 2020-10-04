package utopia.reflection.component.drawing.immutable

import utopia.genesis.color.Color
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

/**
  * Context required when drawing text
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  * @param font Font used when drawing the text
  * @param color Color used when drawing the text
  * @param alignment Alignment used for positioning the text within the target context
  * @param insets Insets placed around the text within the target context
  */
case class TextDrawContext(font: Font, color: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
						   insets: StackInsets = StackInsets.any)
{
	/**
	  * @param other Another text draw context
	  * @return Whether this context has different size-affecting qualities than the other context
	  */
	def hasSameDimensionsAs(other: TextDrawContext) = font == other.font && insets == other.insets
}
