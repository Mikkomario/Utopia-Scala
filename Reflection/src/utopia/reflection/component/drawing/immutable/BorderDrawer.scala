package utopia.reflection.component.drawing.immutable

import utopia.reflection.component.drawing.template
import utopia.reflection.component.drawing.template.DrawLevel.{Foreground, Normal}
import utopia.reflection.shape.Border

/**
  * This custom drawer draws a set of borders inside the component without affecting component layout
  * @author Mikko Hilpinen
  * @since 5.5.2019, v1+
  */
class BorderDrawer(override val border: Border, isAboveContent: Boolean = true) extends template.BorderDrawer
{
	// ATTRIBUTES	------------------
	
	override val drawLevel = if (isAboveContent) Foreground else Normal
}
