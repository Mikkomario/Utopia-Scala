package utopia.reflection.component.drawing.view

import utopia.flow.view.immutable.View
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.{Bounds, Circle}
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Background
import utopia.reflection.event.ButtonState

/**
  * Used for drawing selection state using a simple circle on item background
  * @author Mikko Hilpinen
  * @since 3.8.2019, v1+
  */
case class SelectionCircleViewDrawer(hoverColor: Color, selectedColor: Color, selectionPointer: View[Boolean],
                                     statePointer: View[ButtonState]) extends CustomDrawer
{
	// IMPLEMENTED	--------------------------
	
	override def opaque = false
	
	override def drawLevel = Background
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		// Only draws circle on hover or selection
		val selectionStatus = selectionPointer.value
		val state = statePointer.value
		
		if (selectionStatus || state.isInFocus || state.isMouseOver || state.isPressed)
		{
			// Calculates cirle origin and radius first
			val circle = Circle(bounds.center, (bounds.width min bounds.height) / 2)
			val color = if (selectionStatus) selectedColor else hoverColor
			
			drawer.onlyFill(color).draw(circle)
		}
	}
}
