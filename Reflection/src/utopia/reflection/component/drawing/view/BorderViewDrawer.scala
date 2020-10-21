package utopia.reflection.component.drawing.view

import utopia.flow.event.Changing
import utopia.reflection.component.drawing.template
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.shape.Border

/**
  * Used for drawing a mutable border over a component
  * @author Mikko Hilpinen
  * @since 28.3.2020, v1
  * @constructor Creates a new border drawer that uses specified pointer for reading drawn border
  * @param borderPointer Pointer from which the border is being drawn
  * @param drawLevel Depth where this drawer is used (default = Normal)
  */
case class BorderViewDrawer(borderPointer: Changing[Border], drawLevel: DrawLevel = Normal)
	extends template.BorderDrawerLike
{
	// IMPLEMENTED	--------------------------
	
	override def border = borderPointer.value
}
