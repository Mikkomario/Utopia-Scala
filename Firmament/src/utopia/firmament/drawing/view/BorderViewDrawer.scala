package utopia.firmament.drawing.view

import utopia.firmament.model.Border
import utopia.flow.view.immutable.View
import utopia.firmament.drawing.template
import utopia.genesis.graphics.DrawLevel
import utopia.genesis.graphics.DrawLevel.Normal

/**
  * Used for drawing a mutable border over a component
  * @author Mikko Hilpinen
  * @since 28.3.2020, Reflection v1
  * @constructor Creates a new border drawer that uses specified pointer for reading drawn border
  * @param borderPointer Pointer from which the border is being drawn
  * @param drawLevel Depth where this drawer is used (default = Normal)
  */
case class BorderViewDrawer(borderPointer: View[Border], drawLevel: DrawLevel = Normal)
	extends template.BorderDrawerLike
{
	// IMPLEMENTED	--------------------------
	
	override def border = borderPointer.value
}
