package utopia.reach.form

import utopia.firmament.context.color.ColorContext
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.util.logging.Logger
import utopia.genesis.util.Screen
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{ColorLevel, ColorRole}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.reach.component.factory.{ContextualMixed, Mixed}
import utopia.reach.component.interactive.button.image.ImageButton
import utopia.reach.component.label.image.{ImageAndTextLabel, ImageLabel}
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.template.ReachComponent
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.context.StaticReachContentWindowContext
import utopia.reach.window.ReachWindow

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
    // TODO: Allow custom component with standard positioning
	def apply(context: ColorContext, color: ColorRole = ColorRole.Failure, icon: SingleColorIcon = SingleColorIcon.empty,
	          closeIcon: SingleColorIcon = SingleColorIcon.empty, preferredShade: ColorLevel = Standard)
	         (implicit windowContext: StaticReachContentWindowContext, exc: ExecutionContext, log: Logger): ShowFormNotification =
		apply { (message, component) =>
			// The pop-up background color is based on the component-creation context
			val background = context.current.color.preferring(preferredShade)(color)
			// The window is anchored to the originating component, if one is named
			val windowF = ReachWindow.withContext(windowContext.withBackground(background))
			val window = component match {
				// Case: Originating component available => Anchors the window
				case Some(component) =>
					windowF.anchoredToUsing(Mixed, component, preferredAlignment = Alignment.Right,
						margin = context.margins.small, keepAnchored = false) {
						(_, factories) => notificationComponent(factories, message, icon, closeIcon)
					}
				// Case: Originating component not available
				//       => Anchors to the parent window, or to the bottom right corner of the screen
				case None =>
					val parentWindow = context.windowPointer.value
					val targetBottomRight = parentWindow match {
						// Case: Parent window available => Anchors to its bottom right corner
						case Some(parentWindow) => parentWindow.bottomRight - Vector2D.twice(context.margins.small)
						// Case: Parent window not available => Anchors to the bottom right corner of the screen
						case None => Screen.size.toPoint - Vector2D.twice(context.margins.medium)
					}
					val window = windowF
						.withPositionAfterResize { bounds => targetBottomRight - bounds.size }
						.using(Mixed, parent = parentWindow.map { _.component }) {
							(_, factories) => notificationComponent(factories, message, icon, closeIcon)
						}
					window
			}
			// Closes the window if acted outside
			window.setToCloseOnAnyKeyRelease()
			window.setToCloseOnFocusLost()
			window.setToCloseWhenClickedOutside()
			
			// Displays the window
			window.display()
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
		// TODO: Make vertical margins very small?
		else
			factories(Framing).small.build(Stack) { stackF =>
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
