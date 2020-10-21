package utopia.reflection.component.drawing.view

import utopia.flow.event.Changing
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.localization.LocalizedString

/**
  * Used for drawing changing text
  * @author Mikko Hilpinen
  * @since 17.10.2020, v2
  */
case class TextViewDrawer(textPointer: Changing[LocalizedString], stylePointer: Changing[TextDrawContext],
						  override val drawLevel: DrawLevel = Normal)
	extends utopia.reflection.component.drawing.template.TextDrawerLike
{
	override def drawContext = stylePointer.value
	
	override def text = textPointer.value
}
