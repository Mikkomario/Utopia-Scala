package utopia.reach.component.interactive

import utopia.firmament.component.Window
import utopia.flow.view.template.eventful.Flag

/**
 * A common trait for classes which implement [[CanDisplayPopup]] by wrapping one.
 *
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
trait CanDisplayPopupWrapper extends CanDisplayPopup
{
	// ABSTRACT --------------------------
	
	/**
	 * @return The wrapped interface
	 */
	protected def wrapped: CanDisplayPopup
	
	
	// IMPLEMENTED  ---------------------
	
	override def popupVisibleFlag: Flag = wrapped.popupVisibleFlag
	override def popupHiddenFlag: Flag = wrapped.popupHiddenFlag
	
	override def showPopup(): Option[Window] = wrapped.showPopup()
	override def hidePopup(): Option[Window] = wrapped.hidePopup()
}
