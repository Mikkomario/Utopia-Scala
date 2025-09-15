package utopia.reach.component.interactive

import utopia.firmament.component.Window
import utopia.flow.view.template.eventful.Flag

/**
 * Common trait for interfaces that provide pop-up display & hide functions
 *
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
trait CanDisplayPopup
{
	// ABSTRACT -----------------------------
	
	/**
	 * A pointer which shows whether a pop-up is being displayed
	 */
	def popupVisibleFlag: Flag
	/**
	 * A pointer which contains true while a pop-up is hidden
	 */
	def popupHiddenFlag: Flag
	
	/**
	 * Displays this field's pop-up window, but only if this component is attached to the main component hierarchy
	 * @return The displayed pop-up window. None if this field was not attached to the main component hierarchy.
	 */
	def showPopup(): Option[Window]
	/**
	 * Hides the currently displayed pop-up window
	 * @return The pop-up window. None if no window is open at the moment.
	 */
	def hidePopup(): Option[Window]
	
	
	// COMPUTED	---------------------------------
	
	/**
	 * @return Whether a pop-up window is currently being displayed
	 */
	def showingPopup = popupVisibleFlag.value
	def showingPopup_=(show: Boolean) = {
		if (show) showPopup() else hidePopup()
	}
	/**
	 * @return Whether the pop-up window is currently hidden / not displayed
	 */
	def popupHidden = !showingPopup
	def popupHidden_=(hidden: Boolean) = showingPopup = !hidden
}
