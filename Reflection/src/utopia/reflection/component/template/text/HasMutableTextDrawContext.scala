package utopia.reflection.component.template.text

import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.template.layout.Alignable
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

/**
  * Common trait for components that allow changes to be made on their text drawing context
  * @author Mikko Hilpinen
  * @since 10.4.2023, v2.0
  */
trait HasMutableTextDrawContext extends HasTextDrawContext with Alignable
{
	// ABSTRACT	--------------------------
	
	/**
	  * @param newContext New text drawing context
	  */
	def textDrawContext_=(newContext: TextDrawContext): Unit
	
	
	// IMPLEMENTED	----------------------
	
	override def align(alignment: Alignment) = this.alignment = alignment
	
	
	// COMPUTED	--------------------------
	
	def textInsets_=(newInsets: StackInsets) = mapTextDrawContext { _.copy(insets = newInsets) }
	
	def alignment_=(newAlignment: Alignment) = mapTextDrawContext { _.copy(alignment = newAlignment) }
	
	def font_=(newFont: Font) = mapTextDrawContext { _.copy(font = newFont) }
	
	def textColor_=(newColor: Color) = mapTextDrawContext { _.copy(color = newColor) }
	
	
	// OTHER	--------------------------
	
	/**
	  * Modifies the drawing context used by this text component
	  * @param f New drawing context
	  */
	def mapTextDrawContext(f: TextDrawContext => TextDrawContext) = textDrawContext = f(textDrawContext)
}
