package utopia.firmament.drawing.view

import utopia.firmament.model.GuiElementStatus
import utopia.flow.view.immutable.View
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.{Bounds, Circle}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.template.DrawLevel.Background

/**
  * Used for drawing selection state using a simple circle on item background
  * @author Mikko Hilpinen
  * @since 3.8.2019, Reflection v1+
  */
case class SelectionCircleViewDrawer(hoverColor: Color, selectedColor: Color, selectionPointer: View[Boolean],
                                     statePointer: View[GuiElementStatus])
	extends CustomDrawer
{
	// IMPLEMENTED	--------------------------
	
	override def opaque = false
	
	override def drawLevel = Background
	
	override def draw(drawer: Drawer, bounds: Bounds) = {
		// Only draws circle on hover or selection
		val selectionStatus = selectionPointer.value
		val state = statePointer.value
		
		if (selectionStatus || state.intensity > 0) {
			// Calculates cirle origin and radius first
			val circle = Circle(bounds.center, (bounds.width min bounds.height) / 2)
			val color = if (selectionStatus) selectedColor else hoverColor
			
			drawer.draw(circle)(DrawSettings.onlyFill(color))
		}
	}
}
