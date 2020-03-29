package utopia.reflection.component.drawing.immutable

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.{Bounds, Circle}
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel.Background
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.swing.button.ButtonState

/**
  * Used for drawing selection state using a simple circle on item background
  * @author Mikko Hilpinen
  * @since 3.8.2019, v1+
  */
class SelectionCircleDrawer(val hoverColor: Color, val selectedColor: Color, val isSelected: () => Boolean,
							val currentState: () => ButtonState) extends CustomDrawer
{
	override def drawLevel = Background
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		// Only draws circle on hover or selection
		val selectionStatus = isSelected()
		val state = currentState()
		
		if (selectionStatus || state.isInFocus || state.isMouseOver || state.isPressed)
		{
			// Calculates cirle origin and radius first
			val circle = Circle(bounds.center, (bounds.width min bounds.height) / 2)
			val color = if (selectionStatus) selectedColor else hoverColor
			
			drawer.onlyFill(color).draw(circle)
		}
	}
}
