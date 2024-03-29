package utopia.reflection.component.swing.button

import utopia.firmament.drawing.mutable.MutableCustomDrawable
import utopia.firmament.model.GuiElementStatus
import utopia.genesis.graphics.Drawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * Extended by button implementations that wish to represent their state by using a button state drawer
  * @author Mikko Hilpinen
  * @since 3.8.2019, v1+
  */
trait CustomDrawableButtonLike extends ButtonLike with MutableCustomDrawable
{
	// IMPLEMENTED	----------------------
	
	override protected def updateStyleForState(newState: GuiElementStatus) = repaint()
	
	
	// OTHER	--------------------------
	
	/**
	  * Adds a new stateful custom drawer to this button
	  * @param drawer Drawer to be added
 	  */
	def addCustomDrawer(drawer: ButtonStateDrawer): Unit = addCustomDrawer(new StatefulDrawer(drawer))
	
	
	// NESTED	--------------------------
	
	private class StatefulDrawer(val stateDrawer: ButtonStateDrawer) extends CustomDrawer
	{
		override def opaque = stateDrawer.opaque
		
		override def drawLevel = stateDrawer.drawLevel
		
		override def draw(drawer: Drawer, bounds: Bounds) = stateDrawer.draw(state, drawer, bounds)
	}
}
