package utopia.reach.form

import utopia.firmament.context.color.ColorContext
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.SizeCategory.VerySmall
import utopia.flow.collection.immutable.Pair
import utopia.flow.util.logging.Logger
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{ColorLevel, ColorRole}
import utopia.reach.component.factory.{ContextualMixed, Mixed}
import utopia.reach.component.interactive.button.image.ImageButton
import utopia.reach.component.label.image.{ImageAndTextLabel, ImageLabel}
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.template.ReachComponent
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.context.StaticReachContentWindowContext
import utopia.reach.window.NotificationWindow

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

object ShowFormNotification
{
	// IMPLICIT    ----------------------------
	
	/**
	 * @param f A function that receives 2 parameters:
	 *              1. The component, over / next to which the notification will be displayed
	 *              1. A (custom) error message to display. May be empty.
	 * @return A form notification display logic, wrapping the specified function
	 */
	implicit def apply(f: (LocalizedString, Option[ReachComponent]) => Unit): ShowFormNotification =
		new _ShowFormNotification(f)
		
	
	// OTHER    ------------------------------
	
	/**
	 * A notification logic based on a simple closeable pop-up window next to the applicable field
	 * @param context Component creation context in the hosting window
	 * @param color Notification color role (default = failure)
	 * @param icon Notification icon (default = empty)
	 * @param closeIcon Icon for the close notification -button (default = empty = no close button)
	 * @param preferredShade Preferred background color shade (default = standard)
	 * @param windowContext Component creation context for the pop-up window (implicit)
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @return A notification logic that will display a simple notification
	 */
	def apply(context: ColorContext, color: ColorRole = ColorRole.Failure, icon: SingleColorIcon = SingleColorIcon.empty,
	          closeIcon: SingleColorIcon = SingleColorIcon.empty, preferredShade: ColorLevel = Standard)
	         (implicit windowContext: StaticReachContentWindowContext, exc: ExecutionContext, log: Logger): ShowFormNotification =
		build(context, color, preferredShade) { notificationComponent(_, _, icon, closeIcon) }
	
	/**
	 * A notification logic that displays a pop-up window next to the applicable component
	 * (or in the bottom right corner of the window or screen).
	 * @param context Component creation context in the hosting window
	 * @param color Notification color role (default = failure)
	 * @param preferredShade Preferred background color shade (default = standard)
	 * @param f A function for building the notification contents.
	 *          Accepts two parameters:
	 *              1. Factories for building the component
	 *              1. Message to display
	 * @param windowContext Component creation context for the pop-up window (implicit)
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @return A notification logic that will display a pop-up window
	 */
	def build(context: ColorContext, color: ColorRole = ColorRole.Failure, preferredShade: ColorLevel = Standard)
	         (f: (ContextualMixed[StaticTextContext], LocalizedString) => ReachComponent)
	         (implicit windowContext: StaticReachContentWindowContext, exc: ExecutionContext, log: Logger): ShowFormNotification =
	{
		// Prepares a factory for constructing the notification windows
		lazy val windowF = NotificationWindow.contextual.withBackground(color, preferredShade).closingEasily
		// Creates and displays the notification window
		apply { (message, component) => windowF.build(context, component) { f(_, message) }.display() }
	}
	
	/**
	 * Creates a standard notification component
	 * @param factories Factories for constructing the component
	 * @param message Displayed message (may be empty, if 'icon' is not empty)
	 * @param icon Displayed icon (may be empty, if 'message' is not empty)
	 * @param closeIcon Icon used for the close button (may be empty)
	 * @return Notification component
	 */
	private def notificationComponent(factories: ContextualMixed[StaticTextContext], message: LocalizedString,
	                                  icon: SingleColorIcon, closeIcon: SingleColorIcon): ReachComponent =
	{
		// Case: No close button => Creates a single label
		if (closeIcon.isEmpty) {
			// Case: No icon => Only displays text
			if (icon.isEmpty)
				factories(TextLabel)(message)
			// Case: No message => Only displays the icon
			else if (message.isEmpty)
				factories(ImageLabel)(icon)
			// Case: Displaying message & icon
			else
				factories(ImageAndTextLabel)(icon, message)
		}
		// Case: Includes a close button => Creates a framed stack with 2-3 elements: [ Icon | Message | Close button ]
		else
			factories(Framing).small.withVerticalInsets(VerySmall).build(Stack) { stackF =>
				stackF.centeredRow.related.build(Mixed) { factories =>
					val iconComponent = icon.notEmpty.map { icon => factories(ImageLabel)(icon) }
					val label = message.notEmpty.map { factories(TextLabel)(_) }
					val closeButton = factories(ImageButton).icon(closeIcon) {
						factories.context.windowPointer.value.foreach { _.close() }
					}
					
					Pair(iconComponent, label).flatten :+ closeButton
				}
			}
	}
	
	
	// NESTED   ------------------------------
	
	private class _ShowFormNotification(f: (LocalizedString, Option[ReachComponent]) => Unit)
		extends ShowFormNotification
	{
		override def apply(message: LocalizedString, component: Option[ReachComponent]): Unit = f(message, component)
	}
}

/**
 * Common trait for logic implementations for form notifications
 *
 * @author Mikko Hilpinen
 * @since 07.09.2025, v1.7
 */
trait ShowFormNotification
{
	// ABSTRACT --------------------------
	
	/**
	 * Displays a notification message
	 * @param message A (custom) error message to display. May be empty.
	 * @param component The component, over / next to which the notification will be displayed. Optional.
	 */
	def apply(message: LocalizedString, component: Option[ReachComponent] = None): Unit
	
	
	// OTHER    -------------------------
	
	/**
	 * Displays a notification message over a component
	 * @param component The component, over / next to which the notification will be displayed
	 * @param message A (custom) error message to display. May be empty.
	 */
	def over(component: ReachComponent, message: LocalizedString): Unit = apply(message, Some(component))
}
