package utopia.reflection.component.swing.button

import utopia.firmament.model.GuiElementStatus
import utopia.genesis.graphics.Drawer
import utopia.paradigm.shape.shape2d.Bounds
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
	  * @return Whether this drawer fills the whole target bounds with 100% alpha paint
	  */
	def opaque: Boolean
	
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
	def draw(state: GuiElementStatus, drawer: Drawer, bounds: Bounds): Unit
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Whether this drawer leaves some of the target bounds fully or partially transparent
	  */
	def transparent = !opaque
}
