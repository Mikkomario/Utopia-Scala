package utopia.reach.component.interactive.input.selection

import utopia.firmament.model.enumeration.MouseInteractionState
import utopia.genesis.graphics.{DrawLevel, Drawer}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

object SelectionDrawer
{
	// OTHER    -------------------
	
	/**
	 * Converts a function into a selection drawer
	 * @param level The depth at which the drawing is performed
	 * @param f A function that accepts 6 parameters:
	 *              1. A drawer
	 *              1. Container bounds
	 *              1. Targeted area bounds
	 *              1. Applicable mouse interaction level
	 *              1. Whether the container is the focused component
	 *              1. Whether visualizing selection
	 * @return A selection drawer based on the specified function
	 */
	def apply(level: DrawLevel)
	         (f: (Drawer, Bounds, Bounds, MouseInteractionState, Boolean, Boolean) => Unit): SelectionDrawer =
		new _SelectionDrawer(level, f)
	
	
	// NESTED   ----------------------
	
	private class _SelectionDrawer(override val drawLevel: DrawLevel,
	                               f: (Drawer, Bounds, Bounds, MouseInteractionState, Boolean, Boolean) => Unit)
		extends SelectionDrawer
	{
		override def draw(drawer: Drawer, bounds: Bounds, targetArea: Bounds, mouseInteraction: MouseInteractionState,
		                  hasFocus: Boolean, selected: Boolean): Unit =
			f(drawer, bounds, targetArea, mouseInteraction, hasFocus, selected)
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
	 * @param targetArea Bounds of the targeted container sub-area
	 * @param mouseInteraction The level of mouse interaction applied on top of the selection
	 * @param hasFocus Whether the selection list is currently the focused component
	 * @param selected Whether the targeted area is the selection area.
	 *                 False if only visualizing a state relating to the mouse (hover over or pressed)
	 */
	def draw(drawer: Drawer, bounds: Bounds, targetArea: Bounds, mouseInteraction: MouseInteractionState,
	         hasFocus: Boolean, selected: Boolean): Unit
}
