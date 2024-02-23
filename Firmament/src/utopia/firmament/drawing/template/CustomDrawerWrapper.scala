package utopia.firmament.drawing.template

import utopia.genesis.graphics.{DrawLevel, Drawer}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * Common trait for custom drawers that utilize or manage other custom drawers
  * @author Mikko Hilpinen
  * @since 23/02/2024, v1.3
  */
trait CustomDrawerWrapper extends CustomDrawer
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The wrapped drawer
	  */
	protected def wrapped: CustomDrawer
	
	
	// IMPLEMENTED  --------------------
	
	override def opaque: Boolean = wrapped.opaque
	override def drawLevel: DrawLevel = wrapped.drawLevel
	
	override def draw(drawer: Drawer, bounds: Bounds): Unit = wrapped.draw(drawer, bounds)
}
