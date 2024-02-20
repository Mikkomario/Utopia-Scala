package utopia.firmament.drawing.mutable

import utopia.firmament.drawing.template.TextDrawerLike
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.StackInsets
import utopia.genesis.graphics.DrawLevel2.Normal
import utopia.genesis.graphics.{DrawLevel2, MeasuredText}
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment

/**
  * A mutable implementation of the text drawer interface
  * @author Mikko Hilpinen
  * @since 7.4.2023, Reflection v2.0
  */
class MutableTextDrawer(initialText: MeasuredText, initialContext: TextDrawContext,
                        initialDrawLevel: DrawLevel2 = Normal)
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
	
	override def drawLevel: DrawLevel2 = _drawLevel
	def drawLevel_=(newLevel: DrawLevel2) = _drawLevel = newLevel
	
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
