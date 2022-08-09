package utopia.reflection.component.drawing.mutable

import utopia.paradigm.shape.shape2d.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.template.{CustomDrawable2, CustomDrawer, DrawLevel}

/**
  * This trait is extended by components that allow custom drawing
  * @author Mikko Hilpinen
  * @since 29.4.2019, v1+
  */
trait MutableCustomDrawable extends CustomDrawable2
{
	// ABSTRACT	----------------
	
	/**
	  * Updates this component's custom drawers
	  * @param drawers The new custom drawers for this component
	  */
	def customDrawers_=(drawers: Vector[CustomDrawer]): Unit
	
	
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
	  * @param opaque Whether this drawer fills the whole target bounds with 100% alpha paint
	  *               (can't be seen through). Default = false.
	  * @param f A drawing function
	  */
	def addCustomDrawer(drawLevel: DrawLevel = Normal, opaque: Boolean = false)(f: (Drawer, Bounds) => Unit): Unit =
		addCustomDrawer(CustomDrawer(drawLevel, opaque)(f))
}
