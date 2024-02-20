package utopia.firmament.drawing.view

import utopia.firmament.model.TextDrawContext
import utopia.flow.view.immutable.View
import utopia.genesis.graphics.DrawLevel2
import utopia.genesis.graphics.DrawLevel2.Normal
import utopia.genesis.graphics.MeasuredText

/**
  * Used for drawing changing text
  * @author Mikko Hilpinen
  * @since 17.10.2020, Reflection v2
  */
case class TextViewDrawer(textPointer: View[MeasuredText], stylePointer: View[TextDrawContext],
                          override val drawLevel: DrawLevel2 = Normal)
	extends utopia.firmament.drawing.template.TextDrawerLike
{
	// IMPLEMENTED	----------------------------
	
	override def text = textPointer.value
	override def font = stylePointer.value.font
	override def insets = stylePointer.value.insets
	override def color = stylePointer.value.color
	override def alignment = stylePointer.value.alignment
}
