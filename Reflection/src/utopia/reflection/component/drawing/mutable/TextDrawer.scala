package utopia.reflection.component.drawing.mutable

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.text.Font

object TextDrawer
{
	/**
	  * Creates a new drawer
	  * @param initialText Initial text displayed by this drawer
	  * @param initialContext Initial drawing context used
	  * @param drawLevel Draw level used (default = normal)
	  * @return A new text drawer
	  */
	def apply(initialText: LocalizedString, initialContext: TextDrawContext, drawLevel: DrawLevel = Normal) =
		new TextDrawer(new PointerWithEvents(initialText), new PointerWithEvents(initialContext))
}

/**
  * Used for drawing text over a component. All settings are mutable.
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
class TextDrawer(val textPointer: PointerWithEvents[LocalizedString], val contextPointer: PointerWithEvents[TextDrawContext],
				 override val drawLevel: DrawLevel = Normal)
	extends template.TextDrawer
{
	// COMPUTED	-----------------------------
	
	def text_=(newText: LocalizedString) = textPointer.value = newText
	
	def drawContext_=(newContext: TextDrawContext) = contextPointer.value = newContext
	
	def font_=(newFont: Font) = mapContext { _.copy(font = newFont) }
	
	def color_=(newColor: Color) = mapContext { _.copy(color = newColor) }
	
	def alignment_=(newAlignment: Alignment) = mapContext { _.copy(alignment = newAlignment) }
	
	def insets_=(newInsets: StackInsets) = mapContext { _.copy(insets = newInsets) }
	
	
	// IMPLEMENTED	-------------------------
	
	override def text = textPointer.value
	
	override def drawContext = contextPointer.value
	
	
	// OTHER	-----------------------------
	
	/**
	  * Alters the context used by this drawer when drawing the text
	  * @param f A function for modifying existing context
	  */
	def mapContext(f: TextDrawContext => TextDrawContext) = drawContext = f(drawContext)
}
