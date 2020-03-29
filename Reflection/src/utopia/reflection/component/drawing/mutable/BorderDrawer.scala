package utopia.reflection.component.drawing.mutable

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.reflection.component.drawing.template
import utopia.reflection.component.drawing.template.DrawLevel.{Foreground, Normal}
import utopia.reflection.shape.Border

object BorderDrawer
{
	/**
	  * Creates a new border drawer
	  * @param border Border being drawn (initially)
	  * @param isAboveContent Whether border should be drawn above underlying content (default = true)
	  * @return A new border drawer
	  */
	def apply(border: Border, isAboveContent: Boolean = true) =
		new BorderDrawer(new PointerWithEvents[Border](border), isAboveContent)
}

/**
  * Used for drawing a mutable border over a component
  * @author Mikko Hilpinen
  * @since 28.3.2020, v1
  * @constructor Creates a new border drawer that uses specified pointer for reading drawn border
  * @param borderPointer Pointer from which the border is being drawn
  * @param isAboveContent Whether border should be drawn above underlying content (default = true)
  */
class BorderDrawer(val borderPointer: PointerWithEvents[Border] = new PointerWithEvents[Border](Border.zero),
				   isAboveContent: Boolean = true)
	extends template.BorderDrawer
{
	// ATTRIBUTES	--------------------------
	
	override val drawLevel = if (isAboveContent) Foreground else Normal
	
	
	// IMPLEMENTED	--------------------------
	
	override def border = borderPointer.value
}
