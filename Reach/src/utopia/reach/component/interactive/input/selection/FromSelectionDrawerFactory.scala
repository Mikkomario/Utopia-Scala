package utopia.reach.component.interactive.input.selection

import utopia.firmament.context.color.ColorContextPropsView
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{ColorLevel, ColorRole}

/**
 * Common trait for selection component factories, which may utilize a [[SelectionDrawer]].
 *
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
trait FromSelectionDrawerFactory[+Repr]
{
	// ABSTRACT -----------------------------
	
	/**
	 * @param makeDrawer A function to use for constructing the selection drawer.
	 *                   None if no selection drawer should be used.
	 * @return A copy of this factory using the specified selection drawer constructor
	 */
	def withSelectionDrawerConstructor(makeDrawer: Option[ColorContextPropsView => SelectionDrawer]): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Copy of this factory without a selection drawer.
	 *         Useful when the generated components visualize their selection state independently.
	 */
	def withoutSelectionDrawer = withSelectionDrawerConstructor(None)
	
	/**
	 * @return Copy of this factory using a selection drawer that highlights the selected area's background
	 */
	def highlightingSelectedArea = withSelectionDrawerConstructor { SelectionDrawer.highlight(_) }
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param makeDrawer A function to use for constructing the selection drawer.
	 * @return A copy of this factory using the specified selection drawer constructor
	 */
	def withSelectionDrawerConstructor(makeDrawer: ColorContextPropsView => SelectionDrawer): Repr =
		withSelectionDrawerConstructor(Some(makeDrawer))
	/**
	 * A drawer used for visualizing selection and mouse interaction
	 * @param drawer New selection drawer to use.
	 *               A drawer used for visualizing selection and mouse interaction
	 * @return Copy of this factory with the specified selection drawer
	 */
	def withSelectionDrawer(drawer: SelectionDrawer): Repr = withSelectionDrawerConstructor { _ => drawer }
	
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
		withSelectionDrawerConstructor { SelectionDrawer.fill(_, color, preferredShade, highlightModifier) }
}
