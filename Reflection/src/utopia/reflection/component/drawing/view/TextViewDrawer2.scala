package utopia.reflection.component.drawing.view

import utopia.flow.view.immutable.View
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.genesis.graphics.MeasuredText

/**
  * Used for drawing changing text
  * @author Mikko Hilpinen
  * @since 17.10.2020, v2
  */
case class TextViewDrawer2(textPointer: View[MeasuredText], stylePointer: View[TextDrawContext],
                           override val drawLevel: DrawLevel = Normal)
	extends utopia.reflection.component.drawing.template.TextDrawerLike2
{
	// IMPLEMENTED	----------------------------
	
	override def text = textPointer.value
	override def font = stylePointer.value.font
	override def insets = stylePointer.value.insets
	override def color = stylePointer.value.color
	override def alignment = stylePointer.value.alignment
}
