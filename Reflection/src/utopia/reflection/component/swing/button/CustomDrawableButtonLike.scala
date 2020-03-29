package utopia.reflection.component.swing.button

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer

/**
  * Extended by button implementations that wish to represent their state by using a button state drawer
  * @author Mikko Hilpinen
  * @since 3.8.2019, v1+
  */
trait CustomDrawableButtonLike extends ButtonLike with CustomDrawable
{
	// IMPLEMENTED	----------------------
	
	override protected def updateStyleForState(newState: ButtonState) = repaint()
	
	
	// OTHER	--------------------------
	
	/**
	  * Adds a new stateful custom drawer to this button
	  * @param drawer Drawer to be added
 	  */
	def addCustomDrawer(drawer: ButtonStateDrawer): Unit = addCustomDrawer(new StatefulDrawer(drawer))
	
	
	// NESTED	--------------------------
	
	private class StatefulDrawer(val stateDrawer: ButtonStateDrawer) extends CustomDrawer
	{
		override def drawLevel = stateDrawer.drawLevel
		
		override def draw(drawer: Drawer, bounds: Bounds) = stateDrawer.draw(state, drawer, bounds)
	}
}
