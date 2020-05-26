package utopia.reflection.container.swing.window.dialog.interaction

import utopia.reflection.component.context.{ButtonContextLike, TextContextLike}
import utopia.reflection.component.swing.MultiLineTextView
import utopia.reflection.component.swing.label.ImageLabel
import utopia.reflection.container.stack.StackLayout.Leading
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.window.dialog.interaction.ButtonColor.Fixed
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalizedString
import utopia.reflection.util.Screen

/**
 * A very simple dialog used for displaying a message to the user
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.2
 * @param standardContext Context used in the dialog content
 * @param buttonContext Context used in the dialog button
 * @param title Dialog title
 * @param message Message displayed in this dialog (multiline)
 * @param buttonText Text in dialog close button
 * @param buttonIcon Icon in dialog close button (optional)
 * @param icon Icon next to dialog message (optional)
 */
class MessageDialog(override val standardContext: TextContextLike, buttonContext: ButtonContextLike,
                    override val title: LocalizedString, message: LocalizedString, buttonText: LocalizedString,
                    buttonIcon: Option[SingleColorIcon] = None, icon: Option[SingleColorIcon] = None)
	extends InteractionDialog[Unit]
{
	override protected def buttonContext(buttonColor: ButtonColor, hasIcon: Boolean) = buttonContext
	
	override protected def buttonBlueprints = Vector(new DialogButtonBlueprint[Unit](buttonText, buttonIcon,
		Fixed(buttonContext.buttonColor))({ () => Some(()) -> true }))
	
	override protected def dialogContent =
	{
		implicit val context: TextContextLike = standardContext
		val messageView = MultiLineTextView.contextual(message, Screen.width / 3)
		icon match
		{
			case Some(icon) => Stack.buildRowWithContext(layout = Leading) { s =>
				s += ImageLabel.contextual(icon.singleColorImage)
				s += messageView
			}
			case None => messageView
		}
	}
	
	override protected def defaultResult = ()
}
