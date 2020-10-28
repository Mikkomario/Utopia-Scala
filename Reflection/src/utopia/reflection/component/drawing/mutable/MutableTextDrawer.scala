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
						override val drawLevel: DrawLevel = Normal, allowMultipleLines: Boolean = true)
	extends TextDrawerLike
{
	// ATTRIBUTES	-------------------------
	
	private var rawText = initialText
	private var _text = if (allowMultipleLines) Right(initialText.lines) else Left(initialText)
	
	var drawContext = initialContext
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @return Text currently being drawn
	  */
	def text = rawText
	def text_=(newText: LocalizedString) =
	{
		rawText = newText
		_text = if (allowMultipleLines) Right(newText.lines) else Left(newText)
	}
	
	def font_=(newFont: Font) = mapContext { _.copy(font = newFont) }
	
	def color_=(newColor: Color) = mapContext { _.copy(color = newColor) }
	
	def alignment_=(newAlignment: Alignment) = mapContext { _.copy(alignment = newAlignment) }
	
	def insets_=(newInsets: StackInsets) = mapContext { _.copy(insets = newInsets) }
	
	
	// IMPLEMENTED	-------------------------
	
	override def drawnText = _text
	
	
	// OTHER	-----------------------------
	
	/**
	  * Alters the context used by this drawer when drawing the text
	  * @param f A function for modifying existing context
	  */
	def mapContext(f: TextDrawContext => TextDrawContext) = drawContext = f(drawContext)
}
