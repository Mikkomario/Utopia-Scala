package utopia.reflection.container.swing.window.interaction

import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.BottomRight

object DialogButtonBlueprint
{
	/**
	  * Creates a new dialog close button
	  * @param text Text displayed on the button
	  * @param icon Icon displayed on the button (None if no icon should be displayed, default)
	  * @tparam A Type of dialog result
	  * @return A new close button
	  */
	def closeButton[A](text: LocalizedString, icon: Option[SingleColorIcon] = None) =
		new DialogButtonBlueprint[A](text, icon)(() => None -> true)
	
	/**
	  * Creates a new dialog close button
	  * @param text Text displayed on the button
	  * @param icon Icon displayed on the button
	  * @tparam A Type of dialog result
	  * @return A new close button
	  */
	def closeButton[A](text: LocalizedString, icon: SingleColorIcon): DialogButtonBlueprint[A] =
		closeButton[A](text, Some(icon))
}

/**
  * Used as instructions for creating standard dialog buttons
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  * @param text Text displayed on the generated buttons
  * @param icon Icon to use on the generated buttons (None if no icon should be used, default)
  * @param color Color that should be used on the generated buttons (default = color scheme primary color)
 *  @param location The location where the button should be placed (default = bottom right)
  * @param generateResultOnPress A function for generating dialog close result when this button is pressed. Also
  *                              returns whether the dialog should be closed.
  */
// TODO: Replace this with WindowButtonBluePrint
class DialogButtonBlueprint[+A](val text: LocalizedString, val icon: Option[SingleColorIcon] = None,
								val color: ButtonColor = ButtonColor.primary, val location: Alignment = BottomRight)
                               (val generateResultOnPress: () => (Option[A], Boolean))
