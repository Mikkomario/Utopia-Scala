package utopia.reflection.component.drawing.view

import utopia.flow.view.template.Viewable
import utopia.flow.view.template.eventful.ChangingLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.localization.LocalizedString

/**
  * Used for drawing changing text
  * @author Mikko Hilpinen
  * @since 17.10.2020, v2
  */
case class TextViewDrawer(textPointer: ChangingLike[LocalizedString], stylePointer: Viewable[TextDrawContext],
						  override val drawLevel: DrawLevel = Normal, allowMultipleLines: Boolean = true)
	extends utopia.reflection.component.drawing.template.TextDrawerLike
{
	// ATTRIBUTES	----------------------------
	
	private val drawnTextPointer =
	{
		if (allowMultipleLines)
			textPointer.lazyMap { t => Right(t.lines) }
		else
			textPointer.lazyMap { Left(_) }
	}
	
	
	// COMPUTED	--------------------------------
	
	/**
	  * @return The text currently being drawn
	  */
	def text = textPointer.value
	
	
	// IMPLEMENTED	----------------------------
	
	override def drawContext = stylePointer.value
	
	override def drawnText = drawnTextPointer.value
}
