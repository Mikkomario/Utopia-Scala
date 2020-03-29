package utopia.reflection.component.drawing.mutable

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.template.{CustomDrawer, DrawLevel}

/**
  * This trait is extended by components that allow custom drawing
  * @author Mikko Hilpinen
  * @since 29.4.2019, v1+
  */
trait CustomDrawable
{
	// ABSTRACT	----------------
	
	/**
	  * @return The custom drawers associated with this component
	  */
	def customDrawers: Vector[CustomDrawer]
	/**
	  * Updates this component's custom drawers
	  * @param drawers The new custom drawers for this component
	  */
	def customDrawers_=(drawers: Vector[CustomDrawer])
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
	  * Adds a new custom drawer for this component
	  * @param drawer A custom drawer
	  */
	def addCustomDrawer(drawer: CustomDrawer) = customDrawers :+= drawer
	/**
	  * Removes a custom drawer from this component
	  * @param drawer A custom drawer
	  */
	def removeCustomDrawer(drawer: Any) = customDrawers = customDrawers.filterNot { _ == drawer }
	/**
	  * Clears all custom drawers from this component
	  */
	def clearCustomDrawers() = customDrawers = Vector()
	/**
	  * Wraps a function into a custom drawer and adds it to this component
	  * @param drawLevel Target draw level (default = normal)
	  * @param f A drawing function
	  */
	def addCustomDrawer(drawLevel: DrawLevel = Normal)(f: (Drawer, Bounds) => Unit): Unit = addCustomDrawer(CustomDrawer(drawLevel)(f))
	
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
