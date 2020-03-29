package utopia.reflection.component

import utopia.genesis.color.Color
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.stack.Stackable
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.text.Font

/**
  * Common trait for components that present text
  * @author Mikko Hilpinen
  * @since 10.12.2019, v1+
  */
trait TextComponent extends Stackable with Alignable
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The text currently presented in this component
	  */
	def text: LocalizedString
	
	/**
	  * @return Context for drawing the text within this component
	  */
	def drawContext: TextDrawContext
	/**
	  * @param newContext New text drawing context
	  */
	def drawContext_=(newContext: TextDrawContext)
	
	
	// IMPLEMENTED	----------------------
	
	override def align(alignment: Alignment) = this.alignment = alignment
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return The width of the current text in this component. None if width couldn't be calculated.
	  */
	def textWidth: Option[Int] = textWidth(text.string)
	
	/**
	  * @return The insets around the text in this component
	  */
	def insets = drawContext.insets
	def insets_=(newInsets: StackInsets) = mapDrawContext { _.copy(insets = newInsets) }
	
	/**
	  * @return This component's text alignment
	  */
	def alignment = drawContext.alignment
	def alignment_=(newAlignment: Alignment) = mapDrawContext { _.copy(alignment = newAlignment) }
	
	/**
	  * @return The font used in this component
	  */
	def font = drawContext.font
	def font_=(newFont: Font) = mapDrawContext { _.copy(font = newFont) }
	
	/**
	  * @return The color of the text in this component
	  */
	def textColor = drawContext.color
	def textColor_=(newColor: Color) = mapDrawContext { _.copy(color = newColor) }
	
	
	// OTHER	--------------------------
	
	/**
	  * Modifies the drawing context used by this text component
	  * @param f New drawing context
	  */
	def mapDrawContext(f: TextDrawContext => TextDrawContext) = drawContext = f(drawContext)
}
