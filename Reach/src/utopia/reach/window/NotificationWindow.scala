package utopia.reach.window

import utopia.firmament.context.color.ColorContext
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.Small
import utopia.flow.util.logging.Logger
import utopia.genesis.util.Screen
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorShade, FromColorRoleFactory, FromShadeFactory}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.reach.component.factory.contextual.ReachContentWindowContextualFactory
import utopia.reach.component.factory.{ContextualMixed, Mixed}
import utopia.reach.component.template.ReachComponent
import utopia.reach.component.wrapper.{Creation, WindowCreationResult}
import utopia.reach.context.StaticReachContentWindowContext

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

/**
 * An interface for displaying notifications and other messages, possibly next to specific components
 *
 * @author Mikko Hilpinen
 * @since 09.09.2025, v1.7
 */
object NotificationWindow
{
	// COMPUTED    -----------------------------
	
	/**
	 * @param windowContext Implicit (base) context for creating new notifications
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation to use
	 * @return A new factory for creating notification windows
	 */
	def contextual(implicit windowContext: StaticReachContentWindowContext, exc: ExecutionContext, log: Logger) =
		withContext(windowContext)
		
	
	// IMPLICIT -------------------------------
	
	// Implicitly converts this object to a factory instance, when required implicit context is available
	implicit def objectToFactory(o: NotificationWindow.type)
	                            (implicit windowContext: StaticReachContentWindowContext, exc: ExecutionContext,
	                             log: Logger): NotificationWindowFactory =
		o.contextual
		
	
	// OTHER    -------------------------------
	
	/**
	 * @param context (Base) context for creating new notifications
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation to use
	 * @return A new factory for creating notification windows
	 */
	def withContext(context: StaticReachContentWindowContext)(implicit exc: ExecutionContext, log: Logger) =
		NotificationWindowFactory(context, None, Standard, Alignment.Right, Small, closeEasily = false)
	
	
	// NESTED   -------------------------------
	
	case class NotificationWindowFactory(override val context: StaticReachContentWindowContext,
	                                     background: Option[ColorRole], preferredShade: ColorLevel,
	                                     alignment: Alignment, distanceToComponent: SizeCategory, closeEasily: Boolean)
	                                    (implicit exc: ExecutionContext, log: Logger)
		extends ReachContentWindowContextualFactory[NotificationWindowFactory]
			with FromAlignmentFactory[NotificationWindowFactory] with FromColorRoleFactory[NotificationWindowFactory]
			with FromShadeFactory[NotificationWindowFactory]
	{
		// COMPUTED --------------------------
		
		/**
		 * @return Copy of this factory which constructs windows that close
		 *         when the user interacts with any (other) component
		 */
		def closingEasily = copy(closeEasily = true)
		
		
		// IMPLEMENTED  ----------------------
		
		override def self: NotificationWindowFactory = this
		
		override def withContext(context: StaticReachContentWindowContext): NotificationWindowFactory =
			copy(context = context)
		
		override def apply(alignment: Alignment): NotificationWindowFactory = copy(alignment = alignment)
		
		override def withBackground(role: ColorRole): NotificationWindowFactory = copy(background = Some(role))
		override def withBackground(background: ColorRole, preferredShade: ColorLevel): NotificationWindowFactory =
			copy(background = Some(background), preferredShade = preferredShade)
		
		override def apply(role: ColorRole): NotificationWindowFactory = withBackground(role)
		override def apply(shade: ColorShade): NotificationWindowFactory = preferring(shade)
		
		override def against(color: Color): NotificationWindowFactory = super[FromShadeFactory].against(color)
		
		
		// OTHER    --------------------------
		
		/**
		 * @param shade Preferred window color shade
		 * @return Copy of this factory that prefers the specified background color shade.
		 *         Note: This only affects factories where 'withBackground(ColorRole)' has been called.
		 */
		def preferring(shade: ColorLevel) = copy(preferredShade = shade)
		
		/**
		 * @param distance (Approximate) distance placed between the related component(s) and the created window(s)
		 * @return Copy of this factory that uses the specified distance setting
		 */
		def withDistanceToComponent(distance: SizeCategory) = copy(distanceToComponent = distance)
		
		/**
		 * Creates a notification window next to a component
		 * @param context Component creation context in the hosting window
		 * @param component The component, next to which the notification should be displayed (optional)
		 * @param f A function for building the notification contents.
		 *          Accepts Factories for building the component.
		 * @return A window that also contains the function 'f' results
		 */
		def buildOver[C <: ReachComponent, R](context: ColorContext, component: ReachComponent)
		                                     (f: ContextualMixed[StaticTextContext] => Creation[C, R]): WindowCreationResult[C, R] =
			build[C, R](context, Some(component))(f)
		/**
		 * Creates a notification window next to a component or in the bottom right corner of the window or screen,
		 * if no component is specified.
		 * @param context Component creation context in the hosting window
		 * @param component The component, next to which the notification should be displayed (optional)
		 * @param f A function for building the notification contents.
		 *          Accepts Factories for building the component.
		 * @return A window that also contains the function 'f' results
		 */
		def build[C <: ReachComponent, R](context: ColorContext, component: Option[ReachComponent] = None)
		                                 (f: ContextualMixed[StaticTextContext] => Creation[C, R]): WindowCreationResult[C, R] =
		{
			// The pop-up background color (if applicable) is based on the component-creation context
			val appliedWindowContext = background match {
				case Some(bg) => windowContext.withBackground(context.current.color.preferring(preferredShade)(bg))
				case None => windowContext
			}
			// The window is anchored to the originating component, if one is named
			val windowF = ReachWindow.withContext(appliedWindowContext)
			val window = component match {
				// Case: Originating component available => Anchors the window
				case Some(component) =>
					windowF.anchoredToUsing(Mixed, component, preferredAlignment = alignment,
						margin = context.margins(distanceToComponent), keepAnchored = false) {
						(_, factories) => f(factories)
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
							(_, factories) => f(factories)
						}
					window
			}
			
			// Closes the window if acted outside (optional feature)
			if (closeEasily) {
				window.setToCloseOnAnyKeyRelease()
				window.setToCloseOnFocusLost()
				window.setToCloseWhenClickedOutside()
			}
			
			window
		}
	}
}
