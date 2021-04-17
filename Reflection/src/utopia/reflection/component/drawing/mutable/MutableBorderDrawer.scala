package utopia.reflection.component.drawing.mutable

import utopia.reflection.component.drawing.template.{BorderDrawerLike, DrawLevel}
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.shape.Border

/**
  * Used for drawing a mutable border over a component
  * @author Mikko Hilpinen
  * @since 28.3.2020, v1
  * @constructor Creates a new border drawer that uses specified pointer for reading drawn border
  * @param initialBorder Initially drawn border (default = no border)
  * @param drawLevel Drawing depth used (default = Normal)
  */
class MutableBorderDrawer(initialBorder: Border = Border.zero, override val drawLevel: DrawLevel = Normal)
	extends BorderDrawerLike
{
	// ATTRIBUTES	----------------------------
	
	private var _border = initialBorder
	
	
	// IMPLEMENTED	----------------------------
	
	override def border = _border
	def border_=(newBorder: Border) = _border = newBorder
}
