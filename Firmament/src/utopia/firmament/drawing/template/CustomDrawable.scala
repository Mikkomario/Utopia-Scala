package utopia.firmament.drawing.template

import utopia.genesis.graphics.{DrawLevel, Drawer}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * This trait is extended by components that use custom drawing
  * @author Mikko Hilpinen
  * @since 29.4.2019, Reflection v1
  */
trait CustomDrawable
{
	// ABSTRACT	----------------
	
	/**
	  * @return The custom drawers associated with this component
	  */
	def customDrawers: Seq[CustomDrawer]
	/**
	  * @return The area where this drawable does custom drawing
	  */
	def drawBounds: Bounds
	/**
	  * Redraws this drawable item
	  */
	def repaint(): Unit
	
	
	// OTHER	----------------
	
	/**
	  * Performs the custom draw
	  * @param level Target draw level
	  * @param drawer A drawer that will do the actual drawing
	  */
	def customDraw(level: DrawLevel, drawer: Drawer) = {
		val b = drawBounds
		customDrawers.view.filter { _.drawLevel == level }.foreach { _.draw(drawer, b) }
	}
}
