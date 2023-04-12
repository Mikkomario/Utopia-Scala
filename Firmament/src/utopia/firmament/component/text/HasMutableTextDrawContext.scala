package utopia.firmament.component.text

import utopia.firmament.component.HasMutableAlignment
import utopia.firmament.model.TextDrawContext
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.StackInsets

/**
  * Common trait for components that allow changes to be made on their text drawing context
  * @author Mikko Hilpinen
  * @since 10.4.2023, Reflection v2.0
  */
trait HasMutableTextDrawContext extends HasTextDrawContext with HasMutableAlignment
{
	// ABSTRACT	--------------------------
	
	/**
	  * @param newContext New text drawing context
	  */
	def textDrawContext_=(newContext: TextDrawContext): Unit
	
	
	// IMPLEMENTED	----------------------
	
	override def alignment_=(newAlignment: Alignment) = mapTextDrawContext { _.copy(alignment = newAlignment) }
	
	
	// COMPUTED	--------------------------
	
	def textInsets_=(newInsets: StackInsets) = mapTextDrawContext { _.copy(insets = newInsets) }
	
	def font_=(newFont: Font) = mapTextDrawContext { _.copy(font = newFont) }
	
	def textColor_=(newColor: Color) = mapTextDrawContext { _.copy(color = newColor) }
	
	@deprecated("Please use alignment = alignment instead", "v1.0")
	def align(alignment: Alignment) = this.alignment = alignment
	
	
	// OTHER	--------------------------
	
	/**
	  * Modifies the drawing context used by this text component
	  * @param f New drawing context
	  */
	def mapTextDrawContext(f: TextDrawContext => TextDrawContext) = textDrawContext = f(textDrawContext)
}
