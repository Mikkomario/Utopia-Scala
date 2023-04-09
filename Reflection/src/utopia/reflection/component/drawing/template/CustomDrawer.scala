package utopia.reflection.component.drawing.template

import utopia.genesis.graphics.Drawer3
import utopia.paradigm.shape.shape2d.Bounds
import utopia.reflection.component.drawing.template.DrawLevel.Normal

object CustomDrawer
{
	/**
	  * Wraps a function into custom drawer
	  * @param level The target draw level (default = Normal = Above component contents but below child contents)
	  * @param opaque Whether this drawer fills the whole target bounds with 100% alpha paint
	  *               (can't be seen through). Default = false.
	  * @param f A function
	  * @return A new custom drawer that calls that function
	  */
	def apply(level: DrawLevel = Normal, opaque: Boolean = false)(f: (Drawer3, Bounds) => Unit): CustomDrawer =
		new FunctionalCustomDrawer(level, opaque)(f)
	
	private class FunctionalCustomDrawer(override val drawLevel: DrawLevel, override val opaque: Boolean)
	                                    (f: (Drawer3, Bounds) => Unit) extends CustomDrawer
	{
		override def draw(drawer: Drawer3, bounds: Bounds) = f(drawer, bounds)
	}
}

/**
  * Custom drawers perform custom drawer atop normal component drawing
  * @author Mikko Hilpinen
  * @since 29.4.2019, v1+
  */
trait CustomDrawer
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Whether this drawer fills the whole bounds with 100% alpha paint
	  *         (blocks line of sight to background elements)
	  */
	def opaque: Boolean
	
	/**
	  * @return The level where this drawer will be drawn
	  */
	def drawLevel: DrawLevel
	
	/**
	  * Performs the drawing
	  * @param drawer A drawer used for the drawing (origin located at parent component origin)
	  * @param bounds Draw area bounds
	  */
	def draw(drawer: Drawer3, bounds: Bounds): Unit
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Whether this drawer leaves the target bounds partially or fully transparent
	  *         (I.e. some of the background elements can still be seen afterwards)
	  */
	def transparent = !opaque
}