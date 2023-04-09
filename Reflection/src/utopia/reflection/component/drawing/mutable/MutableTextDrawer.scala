package utopia.reflection.component.drawing.mutable

import utopia.genesis.graphics.MeasuredText
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.template.{DrawLevel, TextDrawerLike}
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

/**
  * A mutable implementation of the text drawer interface
  * @author Mikko Hilpinen
  * @since 7.4.2023, v2.0
  */
class MutableTextDrawer(initialText: MeasuredText, initialContext: TextDrawContext,
                        initialDrawLevel: DrawLevel = Normal)
	extends TextDrawerLike
{
	// ATTRIBUTES   ----------------------
	
	private var _text = initialText
	private var _drawLevel = initialDrawLevel
	
	/**
	  * Text draw context being used (mutable)
	  */
	var context = initialContext
	
	
	// IMPLEMENTED  ----------------------
	
	override def drawLevel: DrawLevel = _drawLevel
	def drawLevel_=(newLevel: DrawLevel) = _drawLevel = newLevel
	
	override def text: MeasuredText = _text
	def text_=(newText: MeasuredText) = _text = newText
	
	override def font: Font = context.font
	def font_=(newFont: Font) = context = context.copy(font = newFont)
	
	override def insets: StackInsets = context.insets
	def insets_=(newInsets: StackInsets) = context = context.copy(insets = newInsets)
	
	override def color: Color = context.color
	def color_=(newColor: Color) = context = context.copy(color = newColor)
	
	override def alignment: Alignment = context.alignment
	def alignment_=(newAlignment: Alignment) = context = context.copy(alignment = newAlignment)
}
