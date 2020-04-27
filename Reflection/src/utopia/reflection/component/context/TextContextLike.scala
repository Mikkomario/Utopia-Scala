package utopia.reflection.component.context

import utopia.genesis.color.Color
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.text.Font

/**
  * A common trait for text context implementations
  * @author Mikko
  * @since 27.4.2020, v
  */
trait TextContextLike extends ColorContextLike
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The font used in texts
	  */
	def font: Font
	
	/**
	  * @return The font used in prompts
	  */
	def promptFont: Font
	
	/**
	  * @return Text alignment used
	  */
	def textAlignment: Alignment
	
	/**
	  * @return Insets / margins placed around drawn text
	  */
	def textInsets: StackInsets
	
	/**
	  * @return Color used in the text
	  */
	def textColor: Color
	
	
	// OTHER	--------------------------
	
	/**
	  * @return Color used in hint texts and disabled elements
	  */
	def hintTextColor = containerBackground.textColorStandard.hintTextColor
}
