package utopia.reflection.container.template.window

import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Primary
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment.BottomRight

import scala.concurrent.Promise
import scala.util.Try

object WindowButtonBlueprint
{
	/**
	  * Creates a blueprint for a button that yields a result and closes the managed window
	  * @param text Text displayed on this button
	  * @param icon Icon displayed on this button (optional)
	  * @param role Color role of this button (default = primary)
	  * @param location Location on the window where this button should be placed (default = bottom right)
	  * @param result A function for generating the result
	  * @tparam A Type of yielded result
	  * @return A new button blueprint
	  */
	def closeWithResult[A](text: LocalizedString, icon: Option[SingleColorIcon] = None, role: ColorRole = Primary,
						   location: Alignment = BottomRight)(result: => A) =
		apply[A](text, icon, role, location) { _.tryComplete(Try { result }) }
}

/**
  * Used as instructions for creating standard dialog buttons
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  * @param text Text displayed on the generated buttons
  * @param icon Icon to use on the generated buttons (None if no icon should be used, default)
  * @param role Color role used by this button (default = primary).
 *  @param location The location where the button should be placed (default = bottom right)
  * @param pressAction A function called when this button is pressed. Accepts a promise that accepts the final
  *                    result and will close the parent window when completed.
  */
case class WindowButtonBlueprint[A](text: LocalizedString, icon: Option[SingleColorIcon] = None,
									 role: ColorRole = Primary, location: Alignment = BottomRight)
									(val pressAction: Promise[A] => Unit)
