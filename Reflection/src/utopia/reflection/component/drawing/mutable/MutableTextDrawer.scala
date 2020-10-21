package utopia.reflection.component.drawing.mutable

import utopia.genesis.color.Color
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.{DrawLevel, TextDrawerLike}
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

/**
  * Used for drawing text over a component. All settings are mutable.
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
class MutableTextDrawer(initialText: LocalizedString, initialContext: TextDrawContext,
						override val drawLevel: DrawLevel = Normal) extends TextDrawerLike
{
	// ATTRIBUTES	-------------------------
	
	var text = initialText
	var drawContext = initialContext
	
	
	// COMPUTED	-----------------------------
	
	def font_=(newFont: Font) = mapContext { _.copy(font = newFont) }
	
	def color_=(newColor: Color) = mapContext { _.copy(color = newColor) }
	
	def alignment_=(newAlignment: Alignment) = mapContext { _.copy(alignment = newAlignment) }
	
	def insets_=(newInsets: StackInsets) = mapContext { _.copy(insets = newInsets) }
	
	
	// OTHER	-----------------------------
	
	/**
	  * Alters the context used by this drawer when drawing the text
	  * @param f A function for modifying existing context
	  */
	def mapContext(f: TextDrawContext => TextDrawContext) = drawContext = f(drawContext)
}
