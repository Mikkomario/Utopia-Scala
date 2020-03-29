package utopia.reflection.component.swing.button

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel

/**
  * Used for drawing different button states
  * @author Mikko Hilpinen
  * @since 3.8.2019, v1+
  */
trait ButtonStateDrawer
{
	// ABSTRACT	---------------------
	
	/**
	  * @return The draw-level this drawer uses
	  */
	def drawLevel: DrawLevel
	
	/**
	  * Draws based on provided state
	  * @param state A button state
	  * @param drawer Used drawer
	  * @param bounds Button bounds
	  */
	def draw(state: ButtonState, drawer: Drawer, bounds: Bounds): Unit
}
