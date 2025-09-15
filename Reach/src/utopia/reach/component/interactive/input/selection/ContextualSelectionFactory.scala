package utopia.reach.component.interactive.input.selection

import utopia.firmament.context.HasContext
import utopia.firmament.context.color.ColorContextPropsView
import utopia.paradigm.color.{ColorLevel, ColorRole}
import utopia.paradigm.color.ColorLevel.Standard

/**
 * Common trait for selection component factories, which may utilize a [[SelectionDrawer]].
 *
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
trait ContextualSelectionFactory[+N <: ColorContextPropsView, +Repr] extends HasContext[N]
{
	// ABSTRACT -----------------------------
	
	/**
	 * A drawer used for visualizing selection and mouse interaction
	 * @param drawer New selection drawer to use.
	 *               A drawer used for visualizing selection and mouse interaction
	 * @return Copy of this factory with the specified selection drawer
	 */
	def withSelectionDrawer(drawer: SelectionDrawer): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Copy of this factory using a selection drawer that highlights the selected area's background
	 */
	def highlightingSelectedArea = withContextualSelectionDrawer { SelectionDrawer.highlight(_) }
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param color Selection background color
	 * @param preferredShade Preferred background shade (default = standard)
	 * @param highlightModifier A modifier applied to highlighting intensity.
	 *                          Highlighting is used for visualizing the mouse hover & focus states.
	 *                          Default = 1.0 = Original.
	 * @return Copy of this factory highlighting the selected area using the specified background color
	 */
	def withSelectionBackground(color: ColorRole, preferredShade: ColorLevel = Standard,
	                            highlightModifier: Double = 1.0) =
		withContextualSelectionDrawer { SelectionDrawer.fill(_, color, preferredShade, highlightModifier) }
	
	/**
	 * @param createDrawer A function that receives this factory's context, and uses it to create a selection drawer
	 * @return Copy of this factory using the constructed selection drawer
	 */
	def withContextualSelectionDrawer(createDrawer: N => SelectionDrawer) =
		withSelectionDrawer(createDrawer(context))
}
