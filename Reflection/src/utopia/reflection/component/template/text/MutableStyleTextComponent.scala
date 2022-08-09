package utopia.reflection.component.template.text

import utopia.paradigm.color.Color
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.template.layout.Alignable
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

/**
  * Common trait for components that present text and allow outside style modifications
  * @author Mikko Hilpinen
  * @since 10.12.2019, v1
  */
trait MutableStyleTextComponent extends TextComponent2 with Alignable
{
	// ABSTRACT	--------------------------
	
	/**
	  * @param newContext New text drawing context
	  */
	def drawContext_=(newContext: TextDrawContext): Unit
	
	
	// IMPLEMENTED	----------------------
	
	override def align(alignment: Alignment) = this.alignment = alignment
	
	
	// COMPUTED	--------------------------
	
	def insets_=(newInsets: StackInsets) = mapDrawContext { _.copy(insets = newInsets) }
	
	def alignment_=(newAlignment: Alignment) = mapDrawContext { _.copy(alignment = newAlignment) }
	
	def font_=(newFont: Font) = mapDrawContext { _.copy(font = newFont) }
	
	def textColor_=(newColor: Color) = mapDrawContext { _.copy(color = newColor) }
	
	
	// OTHER	--------------------------
	
	/**
	  * Modifies the drawing context used by this text component
	  * @param f New drawing context
	  */
	def mapDrawContext(f: TextDrawContext => TextDrawContext) = drawContext = f(drawContext)
}
