package utopia.reflection.container.swing.window

import utopia.firmament.localization.LocalString._
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.flow.operator.filter.Filter
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.action.ActorHandler2
import utopia.genesis.handling.event.keyboard.{KeyStateEvent2, KeyStateListener2, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{CommonMouseEvents, MouseButtonStateEvent2, MouseButtonStateListener2}
import utopia.genesis.handling.template.Handleable2
import utopia.genesis.util.Screen
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.TopLeft
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.ReflectionComponentLike
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.{Never, WhenAnyKeyPressed, WhenClickedOutside, WhenEscPressed, WhenFocusLost}

import java.awt.event.{WindowEvent, WindowFocusListener}
import scala.concurrent.ExecutionContext

/**
  * Used for converting components to pop-ups
  * @author Mikko Hilpinen
  * @since 2.8.2019, v1+
  */
object Popup
{
	/**
	  * Creates a new popup window
	  * @param context Component that wishes to display the popup
	  * @param content Popup contents (stackabe container)
	  * @param actorHandler An actorhandler that will supply the pop-up with action events. These are required in
	  *                     mouse- and keyboard event generating.
	  * @param resizeAlignment Alignment used for handling pop-up position when its size changes.
	  *                        Default = Top Left = Pop up's position wil remain the same
	  * @param autoCloseLogic Logic that determines when this pop-up is automatically closed
	  *                       (default = never close automatically)
	  * @param getTopLeft A function for calculating the new top left corner of the pop-up within context component.
	  *                   Provided parameters are: a) Context size and b) pop-up window size
	  * @tparam C Type of displayed item
	  * @return Newly created and pop-up window
	  */
	def apply[C <: AwtContainerRelated with ReflectionStackable](context: ReflectionComponentLike with AwtComponentRelated,
	                                                             content: C,
	                                                             actorHandler: ActorHandler2,
	                                                             autoCloseLogic: PopupAutoCloseLogic = Never,
	                                                             resizeAlignment: Alignment = TopLeft)
	                                                            (getTopLeft: (Size, Size) => Point)
	                                                            (implicit exc: ExecutionContext) =
	{
		// If context isn't in a window (which it should), has to use a Frame instead of a dialog
		val owner = context.parentWindow
		val windowTitle = "Popup".local("en").localizationSkipped
		val newWindow = Window(content, owner, windowTitle, Program, getAnchor = _.topLeft, borderless = true)
		
		// Calculates the absolute target position
		val newPosition = context.absolutePosition + getTopLeft(context.size, newWindow.size)
		
		// Sets pop-up position, but makes sure it fits into screen
		val screenInsets = Screen.insetsAt(newWindow.component.getGraphicsConfiguration)
		val maxPosition = (Screen.size - newWindow.size - Size(screenInsets.right, screenInsets.bottom)).toPoint
		newWindow.position = newPosition topLeft maxPosition
		
		// Determines auto close logic
		autoCloseLogic match {
			case WhenFocusLost => newWindow.component.addWindowFocusListener(new HideOnFocusLostListener(newWindow))
			case WhenClickedOutside => CommonMouseEvents += new HideOnOutsideClickListener(newWindow)
			case WhenAnyKeyPressed => KeyboardEvents += new HideOnKeyPressListener(newWindow)
			case WhenEscPressed => newWindow.setToCloseOnEsc()
			case _ => ()
		}
		
		newWindow.startEventGenerators(actorHandler)
		newWindow
	}
	
	
	// NESTED	----------------------
	
	/**
	  * A common trait for different automated popup close logic options
	  */
	sealed trait PopupAutoCloseLogic
	
	object PopupAutoCloseLogic
	{
		/**
		  * Popup is never closed automatically
		  */
		case object Never extends PopupAutoCloseLogic
		
		/**
		  * Popup is closed automatically when it first loses focus
		  */
		case object WhenFocusLost extends PopupAutoCloseLogic
		
		/**
		  * Popup is closed automatically when user clicks outside the popup area
		  */
		case object WhenClickedOutside extends PopupAutoCloseLogic
		
		/**
		  * Popup is closed automatically when any key is pressed in any application window
		  */
		case object WhenAnyKeyPressed extends PopupAutoCloseLogic
		
		/**
		  * Popup is closed automatically when esc is pressed while this popup has focus
		  */
		case object WhenEscPressed extends PopupAutoCloseLogic
	}
	
	private class HideOnFocusLostListener(popup: Window[_]) extends WindowFocusListener
	{
		private var hasGainedFocus = false
		
		override def windowGainedFocus(e: WindowEvent) = hasGainedFocus = true
		
		override def windowLostFocus(e: WindowEvent) =
		{
			if (hasGainedFocus)
			{
				popup.component.removeWindowFocusListener(this)
				popup.close()
			}
		}
	}
	
	private trait HideActionListener extends Handleable2
	{
		// ABSTRACT	--------------------------
		
		protected def popup: Window[_]
		
		
		// IMPLEMENTED	----------------------
		
		override def handleCondition: FlagLike = popup.notClosedFlag
	}
	
	private class HideOnOutsideClickListener(override val popup: Window[_])
		extends MouseButtonStateListener2 with HideActionListener
	{
		// ATTRIBUTES	----------------------
		
		private val actionThreshold = Now + 0.1.seconds
		
		override val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent2] =
			MouseButtonStateListener2.filter.pressed
		
		
		// IMPLEMENTED	----------------------
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent2) = {
			if (popup.visible && Now > actionThreshold && !popup.bounds.contains(event.position.absolute))
				popup.close()
		}
	}
	
	private class HideOnKeyPressListener(override val popup: Window[_]) extends KeyStateListener2 with HideActionListener
	{
		override val keyStateEventFilter = KeyStateEvent2.filter.pressed
		
		override def onKeyState(event: KeyStateEvent2) = popup.close()
	}
}
