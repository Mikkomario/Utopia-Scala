package utopia.reflection.component.swing

import java.awt.event.KeyEvent

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButtonStateEvent}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{KeyStateHandlerType, KeyStateListener, MouseButtonStateHandlerType, MouseButtonStateListener}
import utopia.genesis.shape.shape2D.Point
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.{Focusable, Refreshable}
import utopia.reflection.component.input.SelectableWithPointers
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.container.swing.window.{Popup, Window}
import utopia.reflection.container.swing.{Stack, SwitchPanel}
import utopia.reflection.controller.data.StackSelectionManager
import utopia.reflection.shape.{StackLength, StackSize, StackSizeModifier}

import scala.concurrent.ExecutionContext

/**
  * A common trait for custom drop down selection components
  * @author Mikko Hilpinen
  * @since 8.3.2020, v1
  * @param selectionDrawer A custom drawer that highlights the selected item in selection pop-up
  * @param betweenDisplaysMargin Margin placed between selection displays (default = any margin)
  * @param displayStackLayout Stack layout used in selection displays stack (default = fit)
  * @param currentSelectionOptionsPointer Pointer for currently displayed selection options (default = new pointer)
  * @param valuePointer Pointer used for currently selected value (default = new pointer)
  * @param exc Implicit execution context (used in pop-up)
  */
abstract class DropDownFieldLike[A, C <: AwtStackable with Refreshable[A]]
(selectionDrawer: CustomDrawer, betweenDisplaysMargin: StackLength = StackLength.any, displayStackLayout: StackLayout = Fit,
 protected val currentSelectionOptionsPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
 override val valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None))
(implicit exc: ExecutionContext)
	extends StackableAwtComponentWrapperWrapper with SelectableWithPointers[Option[A], Vector[A]] with Focusable
{
	// ABSTRACT	--------------------------------
	
	/**
	  * Checks whether the two selectable items should be considered the same option
	  * @param first The first item
	  * @param second The second item
	  * @return Whether these items represent the same option
	  */
	protected def checkEquals(first: A, second: A): Boolean
	
	/**
	  * Creates a new selection display
	  * @param item Item to display initially
	  * @return A new selection display
	  */
	protected def makeDisplay(item: A): C
	
	/**
	  * @return The primary display for this component. Only component shown when no pop-up is displayed.
	  */
	protected def mainDisplay: AwtStackable with Focusable
	
	/**
	  * @return A display used when there are no items to select when pop-up should be displayed
	  */
	protected def noResultsView: AwtStackable
	
	/**
	  * @return Actor handler used for delivering action events for pop-up and key event handling
	  */
	protected def actorHandler: ActorHandler
	
	
	// ATTRIBUTES	----------------------------
	
	private val searchStack = Stack.column[C](margin = betweenDisplaysMargin, layout = displayStackLayout)
	private val displaysManager = new StackSelectionManager[A, C](searchStack, selectionDrawer, checkEquals,
		currentSelectionOptionsPointer)(makeDisplay)
	/**
	  * The view component that contains the currently displayed content for the pop-up (will be placed within each created pop-up)
	  */
	protected val popupContentView = SwitchPanel[AwtStackable](searchStack)
	
	private var focusGainSkips = 0
	private var visiblePopup: Option[Window[_]] = None
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Whether this field is currently displaying a pop-up view
	  */
	def isDisplayingPopUp = visiblePopup.exists { _.isVisible }
	
	/**
	  * @return Current stack size of the search options -view
	  */
	def currentSearchStackSize = searchStack.stackSize
	
	/**
	  * @return Currently used item displays
	  */
	def currentDisplays = displaysManager.displays
	
	
	// IMPLEMENTED	----------------------------
	
	override protected def wrapped = mainDisplay
	
	override def background_=(color: Color) =
	{
		super.background_=(color)
		searchStack.background = color
		noResultsView.background = color
	}
	
	
	// OTHER	--------------------------------
	
	/**
	  * Sets up interactive events within this component. Should be called once after this component has been initialized.
	  */
	protected def setup(shouldDisplayPopUpOnFocusGain: Boolean) =
	{
		searchStack.background = background
		
		// When the field gains focus, displays the pop-up window (if not yet displayed)
		if (shouldDisplayPopUpOnFocusGain)
		{
			mainDisplay.addFocusGainedListener {
				if (focusGainSkips > 0)
					focusGainSkips -= 1
				else
					displayPopup()
			}
		}
		
		currentSelectionOptionsPointer.addListener({ e =>
			// Displays either "no content" view or the selection stack view
			if (e.newValue.isEmpty)
				popupContentView.set(noResultsView)
			else
			{
				// If only one item is available, auto-selects that one
				if (e.newValue.size == 1)
					value = Some(e.newValue.head)
				popupContentView.set(searchStack)
			}
		}, Some(Vector()))
		if (currentSelectionOptionsPointer.value.isEmpty)
			popupContentView.set(noResultsView)
		
		// When value is updated from an external source, it also affects selection stack
		addValueListener({ e => displaysManager.value = e.newValue }, Some(None))
		
		// When display manager updates its value (for example due to content change), updates current value
		// (if not displaying a pop-up at the time)
		displaysManager.addValueListener { e =>
			if (!isDisplayingPopUp)
				value = e.newValue
		}
		
		displaysManager.enableMouseHandling()
		displaysManager.enableKeyHandling(actorHandler, listenEnabledCondition = Some(() => mainDisplay.isInFocus ||
			visiblePopup.exists { _.isVisible }))
		
		addKeyStateListener(ShowPopupKeyListener)
		addMouseButtonListener(ShowPopupKeyListener)
	}
	
	private def displayPopup() =
	{
		if (visiblePopup.isEmpty)
		{
			// Creates and displays the popup
			val popup = Popup(mainDisplay, popupContentView, actorHandler) {
				(fieldSize, _) => Point(0, fieldSize.height) }
			visiblePopup = Some(popup)
			// Relays key events to the search field
			popup.addConstraint(PopUpWidthModifier)
			popup.relayAwtKeyEventsTo(mainDisplay)
			popup.addKeyStateListener(KeyStateListener(
				KeyStateEvent.keysFilter(KeyEvent.VK_TAB, KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE) &&
					KeyStateEvent.wasPressedFilter) { _ => if (popup.isFocusedWindow) popup.close() })
			popup.addMouseButtonListener(MouseButtonStateListener(MouseButtonStateEvent.wasReleasedFilter){ _ =>
				if (popup.isFocusedWindow)
					popup.close()
				None
			})
			popup.display()
			popup.closeFuture.foreach { _ =>
				focusGainSkips += 1
				visiblePopup = None
				// Updates "real" selection only when pop-up closes
				value = displaysManager.value
			}
		}
	}
	
	
	// NESTED	-------------------------------
	
	private object ShowPopupKeyListener extends KeyStateListener with Handleable with MouseButtonStateListener
	{
		private def isReceivingEvents = visiblePopup.isEmpty
		
		override def allowsHandlingFrom(handlerType: HandlerType) = handlerType match
		{
			case KeyStateHandlerType => isReceivingEvents && mainDisplay.isInFocus
			case MouseButtonStateHandlerType => isReceivingEvents
			case _ => super.allowsHandlingFrom(handlerType)
		}
		
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter &&
			KeyStateEvent.notKeysFilter(Vector(KeyEvent.VK_ESCAPE, KeyEvent.VK_TAB))
		
		override def onKeyState(event: KeyStateEvent) = displayPopup()
		
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter && (e => e.isOverArea(mainDisplay.bounds))
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			// Grabs focus if possible
			if (!mainDisplay.isInFocus)
				mainDisplay.requestFocusInWindow()
			displayPopup()
			Some(ConsumeEvent("Search From Field Clicked"))
		}
	}
	
	private object PopUpWidthModifier extends StackSizeModifier
	{
		// Stack size width must be at least the current width of the main field
		override def apply(size: StackSize) = size.mapWidth { _.mapOptimal { _ max mainDisplay.width } }
	}
}
