package utopia.reach.component.interactive.input.selection

import utopia.firmament.context.color.ColorContextPropsView
import utopia.firmament.model.enumeration.MouseInteractionState
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.DrawLevel.Background
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer}
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

object SelectionDrawer
{
	// OTHER    -------------------
	
	/**
	 * @param context Context in which drawing is performed
	 * @param modifier A modifier applied to the highlight intensity.
	 *                 Default = 1.0 = Original.
	 * @return A drawer that highlights the selected & mouse hover areas
	 */
	def highlight(context: ColorContextPropsView, modifier: Double = 1.0): SelectionDrawer =
		new DefaultSelectionDrawer(context, highlightMod = modifier)
	/**
	 * @param context Context in which drawing is performed
	 * @param color Selection fill color
	 * @param preferredShade Preferred selection fill shade (default = standard)
	 * @param highlightModifier A modifier applied to highlighting intensity.
	 *                          Highlighting is used for visualizing the mouse hover & focus states.
	 *                          Default = 1.0 = Original.
	 * @return A drawer that fills the selected area using the specified color
	 */
	def fill(context: ColorContextPropsView, color: ColorRole, preferredShade: ColorLevel = Standard,
	         highlightModifier: Double = 1.0): SelectionDrawer =
		new DefaultSelectionDrawer(context, Some(color), preferredShade, highlightModifier)
	
	/**
	 * Converts a function into a selection drawer
	 * @param level The depth at which the drawing is performed
	 * @param selectionBackgroundPointer A pointer that contains the drawn background color, if applicable.
	 *                                   None if no solid background is drawn (default).
	 * @param f A function that accepts 6 parameters:
	 *              1. A drawer
	 *              1. Container bounds
	 *              1. Targeted area bounds
	 *              1. Applicable mouse interaction level
	 *              1. Whether the container is the focused component
	 *              1. Whether visualizing selection
	 * @return A selection drawer based on the specified function
	 */
	def apply(level: DrawLevel, selectionBackgroundPointer: Option[Changing[Color]] = None)
	         (f: (Drawer, Bounds, Bounds, MouseInteractionState, Boolean, Boolean) => Unit): SelectionDrawer =
		new _SelectionDrawer(level, selectionBackgroundPointer, f)
	
	
	// NESTED   ----------------------
	
	private class DefaultSelectionDrawer(context: ColorContextPropsView, color: Option[ColorRole] = None,
	                                     preferredShade: ColorLevel = Standard, highlightMod: Double = 1.0)
		extends SelectionDrawer
	{
		// ATTRIBUTES   --------------
		
		override val drawLevel: DrawLevel = Background
		private val bgP = Changing.lazily {
			color match {
				case Some(color) => context.colorPointer.preferring(preferredShade)(color)
				case None => context.backgroundPointer.map { _.highlighted }
			}
		}
		override val selectionBackgroundPointer: Option[Changing[Color]] = Some(bgP)
		
		
		// IMPLEMENTED  --------------
		
		override def draw(drawer: Drawer, bounds: Bounds, targetArea: Bounds, mouseInteraction: MouseInteractionState,
		                  hasFocus: Boolean, selected: Boolean): Unit =
		{
			if (targetArea.nonEmpty) {
				// Determines the fill color to use
				implicit val ds: DrawSettings = {
					// Applies highlighting based on the target state
					val highlightFactor = {
						val selectionMod = {
							// Case: Displaying selection with highlighting => Highlights by 2 levels
							if (selected && color.isEmpty)
								2
							// Case: Displaying hover or selection with color => No highlight
							else
								0
						}
						// Also applies highlighting from focus & mouse interaction
						val focusMod = if (hasFocus) 1 else 0
						(selectionMod + mouseInteraction.level + focusMod) * highlightMod
					}
					// Determines the draw color based on highlighting, overlay color and contextual background
					val bg = context.backgroundPointer.value
					(if (selected) color else None) match {
						// Case: Drawing using a specific color
						//       => Uses a version of that color that suits the current background
						case Some(color) =>
							val default = context.colors(color).against(bg, preferredShade)
							// Case: No highlighting
							if (highlightFactor == 0)
								default
							// Case: Highlighting needs to be applied as well
							//       => Checks whether to lighten or to darken the color
							else {
								val bgLight = bg.relativeLuminance
								if (default.relativeLuminance >= bgLight)
									default.lightenedBy(highlightFactor)
								else
									default.darkenedBy(highlightFactor)
							}
						// Case: Drawing using a highlighted background color
						case None => bg.highlightedBy(highlightFactor)
					}
				}
				// Performs the actual drawing
				drawer.draw(targetArea + bounds.position)
			}
		}
	}
	
	private class _SelectionDrawer(override val drawLevel: DrawLevel,
	                               override val selectionBackgroundPointer: Option[Changing[Color]],
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
	 * @return A pointer that contains the drawn background color, if applicable.
	 *         None if no solid background is drawn.
	 */
	def selectionBackgroundPointer: Option[Changing[Color]]
	
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
