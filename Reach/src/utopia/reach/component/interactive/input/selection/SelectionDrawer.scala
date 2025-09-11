package utopia.reach.component.interactive.input.selection

import utopia.genesis.graphics.{DrawLevel, Drawer}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

object SelectionDrawer
{
	// OTHER    -------------------
	
	/**
	 * Converts a function into a selection drawer
	 * @param level The depth at which the drawing is performed
	 * @param f A function that accepts 3 parameters:
	 *              1. A drawer
	 *              1. Container bounds
	 *              1. Selected area bounds
	 * @return A selection drawer based on the specified function
	 */
	def apply(level: DrawLevel)(f: (Drawer, Bounds, Bounds) => Unit): SelectionDrawer = new _SelectionDrawer(level, f)
	
	
	// NESTED   ----------------------
	
	private class _SelectionDrawer(override val drawLevel: DrawLevel, f: (Drawer, Bounds, Bounds) => Unit)
		extends SelectionDrawer
	{
		override def draw(drawer: Drawer, bounds: Bounds, selectedArea: Bounds): Unit = f(drawer, bounds, selectedArea)
	}
}

/**
 * Used for visualizing selected components on the container level
 *
 * @author Mikko Hilpinen
 * @since 10.09.2025, v1.7
 */
trait SelectionDrawer
{
	/**
	 * @return The level at which the drawing is performed
	 */
	def drawLevel: DrawLevel
	
	/**
	 * Visualizes the selected area
	 * @param drawer Drawer to utilize
	 * @param bounds Container bounds
	 * @param selectedArea Bounds of the selected container sub-area
	 */
	def draw(drawer: Drawer, bounds: Bounds, selectedArea: Bounds): Unit
}
