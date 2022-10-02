package utopia.reflection.container.swing.window.interaction

import utopia.genesis.util.Screen
import utopia.reflection.component.context.{ButtonContextLike, TextContext, TextContextLike}
import utopia.reflection.component.swing.display.MultiLineTextView
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.{LocalizedString, Localizer}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center

object YesNoWindow
{
	/**
	  * Creates a new yes-no-question-dialog
	  * @param textContext Context used when displaying the question text
	  * @param title Title for this dialog
	  * @param question Question displayed on this dialog
	  * @param icons Icons for yes and no buttons (default = empty)
	  * @param colors Color overrides for yes and no buttons (default = empty)
	  * @param defaultResult Result generated when the user simply closes this dialog (default = false = No)
	  * @return A new dialog ready to be displayed
	  */
	def apply(textContext: TextContext, title: LocalizedString, question: LocalizedString,
			  icons: Map[Boolean, SingleColorIcon] = Map(), colors: Map[Boolean, ButtonColor] = Map(),
			  defaultResult: Boolean = false) =
	{
		val buttonTextAlign = if (icons.isEmpty) Center else Alignment.Left
		new YesNoWindow(textContext, title, question, icons, colors, defaultResult)({ (color, _) =>
			textContext.withTextAlignment(buttonTextAlign).forButtons(color) })
	}
}

/**
  * These dialogs are used for checking whether user agrees or disagrees with a statement
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  * @param standardContext Context used when displaying the question text
  * @param title Title for this dialog
  * @param question Question displayed on this dialog
  * @param icons Icons for yes and no buttons (default = empty)
  * @param colors Color overrides for yes and no buttons (default = empty)
  * @param defaultResult Result generated when the user simply closes this dialog (default = false = No)
  * @param getButtonContext A function for creating button creation context based on specified button color and
  *                         whether the button shall have an icon.
  */
// TODO: Could utilize UncertainBoolean
class YesNoWindow(override val standardContext: TextContextLike, override val title: LocalizedString,
                  val question: LocalizedString, icons: Map[Boolean, SingleColorIcon] = Map(),
                  colors: Map[Boolean, ButtonColor] = Map(), override val defaultResult: Boolean = false)
                 (getButtonContext: (ButtonColor, Boolean) => ButtonContextLike) extends InteractionWindow[Boolean]
{
	private implicit val context: TextContextLike = standardContext
	private implicit val languageCode: String = "en"
	private implicit val localizer: Localizer = standardContext.localizer
	
	override protected def buttonContext(buttonColor: ButtonColor, hasIcon: Boolean) =
		getButtonContext(buttonColor, hasIcon)
	
	override protected def buttonBlueprints =
	{
		val yesButton = new DialogButtonBlueprint[Boolean]("Yes", icons.get(true),
			colors.getOrElse(true, ButtonColor.secondary))(() => Some(true) -> true)
		val noButton = new DialogButtonBlueprint[Boolean]("No", icons.get(false),
			colors.getOrElse(false, ButtonColor.primary))(() => Some(false) -> true)
		Vector(yesButton, noButton)
	}
	
	override protected def dialogContent = MultiLineTextView.contextual(question, Screen.size.width / 3)
}
