package utopia.reflection.component.drawing.template

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer

/**
  * This trait is extended by components that use custom drawing
  * @author Mikko Hilpinen
  * @since 29.4.2019, v1
  */
trait CustomDrawable2
{
	// ABSTRACT	----------------
	
	/**
	  * @return The custom drawers associated with this component
	  */
	def customDrawers: Vector[CustomDrawer]
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
	def customDraw(level: DrawLevel, drawer: Drawer) =
	{
		val b = drawBounds
		customDrawers.filter { _.drawLevel == level }.foreach { d => drawer.withCopy { d.draw(_, b) } }
	}
}
