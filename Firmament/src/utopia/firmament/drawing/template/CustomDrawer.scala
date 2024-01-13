package utopia.firmament.drawing.template

import utopia.genesis.graphics.Drawer
import utopia.firmament.drawing.template.DrawLevel.Normal
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

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
	def apply(level: DrawLevel = Normal, opaque: Boolean = false)(f: (Drawer, Bounds) => Unit): CustomDrawer =
		new FunctionalCustomDrawer(level, opaque)(f)
	
	private class FunctionalCustomDrawer(override val drawLevel: DrawLevel, override val opaque: Boolean)
	                                    (f: (Drawer, Bounds) => Unit) extends CustomDrawer
	{
		override def draw(drawer: Drawer, bounds: Bounds) = f(drawer, bounds)
	}
}

/**
  * Custom drawers perform custom drawer atop normal component drawing
  * @author Mikko Hilpinen
  * @since 29.4.2019, Reflection v1+
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
	  * @param bounds Draw area bounds, which match the component bounds.
	  *               For optimized drawing, also consider drawer.clippingBounds
	  */
	def draw(drawer: Drawer, bounds: Bounds): Unit
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Whether this drawer leaves the target bounds partially or fully transparent
	  *         (I.e. some of the background elements can still be seen afterwards)
	  */
	def transparent = !opaque
}