package utopia.reflection.container.swing.window.interaction

import utopia.firmament.context.TextContext
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.enumeration.StackLayout.Leading
import utopia.genesis.util.Screen
import utopia.reflection.component.swing.display.MultiLineTextView
import utopia.reflection.component.swing.label.ImageLabel
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.interaction.ButtonColor.Fixed
import utopia.firmament.localization.LocalizedString

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
class MessageWindow(override val standardContext: TextContext, buttonContext: TextContext,
                    override val title: LocalizedString, message: LocalizedString, buttonText: LocalizedString,
                    buttonIcon: Option[SingleColorIcon] = None, icon: Option[SingleColorIcon] = None)
	extends InteractionWindow[Unit]
{
	override protected def buttonContext(buttonColor: ButtonColor, hasIcon: Boolean) = buttonContext
	
	override protected def buttonBlueprints =
		Vector(new DialogButtonBlueprint[Unit](buttonText, buttonIcon,
			Fixed(buttonContext.background))({ () => Some(()) -> true }))
	
	override protected def dialogContent =
	{
		implicit val context: StaticTextContext = standardContext
		val messageView = MultiLineTextView.contextual(message, Screen.width / 3)
		icon match {
			case Some(icon) => Stack.buildRowWithContext(layout = Leading) { s =>
				s += ImageLabel.contextual(icon)
				s += messageView
			}
			case None => messageView
		}
	}
	
	override protected def defaultResult = ()
}
