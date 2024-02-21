package utopia.reflection.component.swing.input

import utopia.firmament.component.display.Refreshable
import utopia.firmament.component.input.SelectableWithPointers
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.stack.modifier.StackSizeModifier
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.consume.ConsumeChoice.Consume
import utopia.genesis.handling.event.keyboard.Key.{Enter, Esc, Tab}
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.MouseButtonStateListener2.MouseButtonStateEventFilter
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent2, MouseButtonStateListener2}
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.template.Focusable
import utopia.reflection.container.swing.layout.multi.AnimatedStack
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.layout.wrapper.SwitchPanel
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.WhenFocusLost
import utopia.reflection.container.swing.window.{Popup, Window}
import utopia.reflection.controller.data.ContainerSelectionManager

import scala.concurrent.ExecutionContext

/**
  * A common trait for custom drop down selection components
  * @author Mikko Hilpinen
  * @since 8.3.2020, v1
  * @param actorHandler Actor handler used for delivering action events for pop-up and key event handling
  * @param selectionDrawer A custom drawer that highlights the selected item in selection pop-up
  * @param betweenDisplaysMargin Margin placed between selection displays (default = any margin)
  * @param displayStackLayout Stack layout used in selection displays stack (default = fit)
  * @param currentSelectionOptionsPointer Pointer for currently displayed selection options (default = new pointer)
  * @param valuePointer Pointer used for currently selected value (default = new pointer)
  * @param exc Implicit execution context (used in pop-up)
  */
abstract class DropDownFieldLike[A, C <: AwtStackable with Refreshable[A]]
(actorHandler: ActorHandler, selectionDrawer: CustomDrawer, betweenDisplaysMargin: StackLength = StackLength.any,
 displayStackLayout: StackLayout = Fit,
 protected val currentSelectionOptionsPointer: EventfulPointer[Vector[A]] = new EventfulPointer[Vector[A]](Vector()),
 override val valuePointer: EventfulPointer[Option[A]] = new EventfulPointer[Option[A]](None),
 contentIsStateless: Boolean = false)
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
	protected def representSameInstance(first: A, second: A): Boolean
	
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
	
	
	// ATTRIBUTES	----------------------------
	
	private val searchStack = new AnimatedStack[C](actorHandler, Y, betweenDisplaysMargin, layout = displayStackLayout)
		//Stack.column[C](margin = betweenDisplaysMargin, layout = displayStackLayout)
	private val displaysManager = new ContainerSelectionManager[A, C](searchStack, selectionDrawer,
		currentSelectionOptionsPointer, representSameInstance,
		if (contentIsStateless) None else Some((a, b) => a == b))(makeDisplay)
	/**
	  * The view component that contains the currently displayed content for the pop-up (will be placed within each created pop-up)
	  */
	protected val popupContentView = SwitchPanel[AwtStackable](searchStack)
	
	private var focusGainSkips = 0
	private val visiblePopupPointer = EventfulPointer.empty[Window[_]]()
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Whether this field is currently displaying a pop-up view
	  */
	def isDisplayingPopUp = visiblePopupPointer.value.exists { _.visible }
	
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
		
		currentSelectionOptionsPointer.addContinuousListenerAndSimulateEvent(Vector()) { e =>
			// Displays either "no content" view or the selection stack view
			if (e.newValue.isEmpty)
				popupContentView.set(noResultsView)
			else
			{
				// If only one item is available, auto-selects that one
				if (e.newValue.size == 1)
				{
					if (isDisplayingPopUp)
						displaysManager.value = Some(e.newValue.head)
					else
						value = Some(e.newValue.head)
				}
				popupContentView.set(searchStack)
			}
		}
		if (currentSelectionOptionsPointer.value.isEmpty)
			popupContentView.set(noResultsView)
		
		// When value is updated from an external source, it also affects selection stack
		valuePointer.addContinuousListenerAndSimulateEvent(None) { e => displaysManager.value = e.newValue }
		
		// When display manager updates its value (for example due to content change), updates current value
		// (if not displaying a pop-up at the time)
		displaysManager.valuePointer.addContinuousListener { e =>
			if (!isDisplayingPopUp)
				value = e.newValue
		}
		
		displaysManager.enableMouseHandling()
		displaysManager.enableKeyHandling(actorHandler,
			listenEnabledCondition = mainDisplay.isInFocus || visiblePopupPointer.value.exists { _.visible })
		
		addStackHierarchyChangeListener(attached => {
			if (attached)
				KeyboardEvents += ShowPopupKeyListener
			else
				KeyboardEvents -= ShowPopupKeyListener
		}, callIfAttached = true)
		addMouseButtonListener(ShowPopupKeyListener)
	}
	
	private def displayPopup() = {
		if (visiblePopupPointer.value.isEmpty) {
			// Creates and displays the popup
			val popup = Popup(mainDisplay, popupContentView, actorHandler, WhenFocusLost) {
				(fieldSize, _) => Point(0, fieldSize.height)
			}
			visiblePopupPointer.value = Some(popup)
			// Relays key events to the search field
			popup.addConstraint(PopUpWidthModifier)
			popup.relayAwtKeyEventsTo(mainDisplay)
			popup.addKeyStateListener(
				KeyStateListener.pressed(Tab, Enter, Esc) { _ => if (popup.isFocusedWindow) popup.close() })
			popup.addMouseButtonListener(MouseButtonStateListener2.released { _ =>
				if (popup.isFocusedWindow)
					popup.close()
			})
			popup.display()
			popup.closeFuture.foreach { _ =>
				focusGainSkips += 1
				visiblePopupPointer.value = None
				// Updates "real" selection only when pop-up closes
				value = displaysManager.value
			}
		}
	}
	
	
	// NESTED	-------------------------------
	
	private object ShowPopupKeyListener extends KeyStateListener with Handleable with MouseButtonStateListener2
	{
		// ATTRIBUTES   ----------------------
		
		override val handleCondition: FlagLike = visiblePopupPointer.map { _.isEmpty }
		
		override val keyStateEventFilter = KeyStateEvent.filter.pressed && !KeyStateEvent.filter(Set(Esc, Tab))
		override val mouseButtonStateEventFilter =
			MouseButtonStateEvent2.filter.leftPressed && MouseButtonStateEventFilter.over(mainDisplay.bounds)
		
		
		// IMPLEMENTED  -----------------------
		
		override def onKeyState(event: KeyStateEvent) = {
			if (mainDisplay.isInFocus)
				displayPopup()
		}
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent2) = {
			// Grabs focus if possible
			if (!mainDisplay.isInFocus)
				mainDisplay.requestFocusInWindow()
			displayPopup()
			Consume("Search From Field Clicked")
		}
	}
	
	private object PopUpWidthModifier extends StackSizeModifier
	{
		// Stack size width must be at least the current width of the main field
		override def apply(size: StackSize) = size.mapWidth { _.mapOptimal { _ max mainDisplay.width } }
	}
}
