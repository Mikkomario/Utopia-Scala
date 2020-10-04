package utopia.reflection.component.template.text

import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.template.layout.stack.Stackable2
import utopia.reflection.localization.LocalizedString

/**
  * Common trait for components that present text
  * @author Mikko Hilpinen
  * @since 10.12.2019, v1
  */
trait TextComponent2 extends Stackable2
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
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return The width of the current text in this component
	  */
	def textWidth = textWidthWith(text.string)
	
	/**
	  * @return The height of the curent text in this component
	  */
	def textHeight = textHeightWith(font)
	
	/**
	  * @return The insets around the text in this component
	  */
	def insets = drawContext.insets
	
	/**
	  * @return This component's text alignment
	  */
	def alignment = drawContext.alignment
	
	/**
	  * @return The font used in this component
	  */
	def font = drawContext.font
	
	/**
	  * @return The color of the text in this component
	  */
	def textColor = drawContext.color
	
	
	// OTHER	---------------------------
	
	/**
	  * @param text Text
	  * @return Width of that text inside this component
	  */
	def textWidthWith(text: String): Int = textWidthWith(font, text)
}
