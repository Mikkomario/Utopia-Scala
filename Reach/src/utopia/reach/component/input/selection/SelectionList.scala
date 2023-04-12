package utopia.reach.component.input.selection

import utopia.firmament.component.display.Refreshable
import utopia.firmament.component.input.SelectionWithPointers
import utopia.firmament.context.ColorContext
import utopia.firmament.controller.data.{ContainerSingleSelectionManager, SelectionKeyListener}
import utopia.firmament.drawing.mutable.{MutableCustomDrawable, MutableCustomDrawableWrapper}
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.EqualsFunction
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.event.{Consumable, ConsumeEvent, MouseButtonStateEvent, MouseMoveEvent}
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{MouseButtonStateHandlerType, MouseButtonStateListener, MouseMoveListener}
import utopia.genesis.view.{GlobalKeyboardEventHandler, GlobalMouseEventHandler}
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.color.{Color, ColorShade}
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.focus.MutableFocusable
import utopia.reach.component.template.{CursorDefining, ReachComponent, ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.MutableStack
import utopia.reach.cursor.Cursor
import utopia.reach.cursor.CursorType.{Default, Interactive}
import utopia.reach.focus.{FocusListener, FocusStateTracker}
import utopia.reach.util.Priority.High
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.shape.stack.StackLength

object SelectionList extends ContextInsertableComponentFactoryFactory[ColorContext, SelectionListFactory,
	ContextualSelectionListFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new SelectionListFactory(hierarchy)
}

class SelectionListFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContext, ContextualSelectionListFactory]
{
	override def withContext[N <: ColorContext](context: N) =
		ContextualSelectionListFactory(this, context)
	
	/**
	  * Creates a new list
	  * @param actorHandler Actor handler that will deliver action events for arrow key handling
	  * @param contextBackgroundPointer A pointer to the background color of this list's container / context
	  * @param contentPointer A pointer to the selection options displayed on this list
	  * @param valuePointer A pointer to the currently selected value (default = new empty pointer)
	  * @param direction Direction along which the selection items are laid (default = Y = vertical)
	  * @param layout Stack layout used for determining display breadth (default = Fit = all have same breadth)
	  * @param margin Margin placed between selectable items (default = any, preferring 0)
	  * @param cap Cap placed at both ends of this list (default = always 0)
	  * @param highlightModifier A modifier applied for selection and focus color highlights (default = 1.0)
	  * @param sameItemCheck A function for testing whether two items should be considered equal
	  *                      (specify only if equals method should <b>not</b> be used) (default = None)
	  * @param alternativeKeyCondition A function that returns true in cases where selection key events should be
	  *                                enabled. Key events are always enabled while this list is in focus.
	  *                                Default = false = Key events are received only while in focus.
	  * @param makeDisplay A function for creating a new display component. Accepts parent component hierarchy and
	  *                    the initially displayed item.
	  * @tparam A Type of displayed / selected value
	  * @tparam C Type of display component
	  * @tparam P Type of selection pool pointer
	  * @return A new list
	  */
	def apply[A, C <: ReachComponentLike with Refreshable[A], P <: Changing[Vector[A]]]
	(actorHandler: ActorHandler, contextBackgroundPointer: View[Color], contentPointer: P,
	 valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None), direction: Axis2D = Y,
	 layout: StackLayout = Fit, margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixedZero,
	 highlightModifier: Double = 1.0, sameItemCheck: Option[EqualsFunction[A]] = None,
	 alternativeKeyCondition: => Boolean = false)
	(makeDisplay: (ComponentHierarchy, A) => C) =
		new SelectionList[A, C, P](parentHierarchy, actorHandler, contextBackgroundPointer, contentPointer,
			valuePointer, direction, layout, margin, cap, highlightModifier, sameItemCheck,
			alternativeKeyCondition)(makeDisplay)
}

case class ContextualSelectionListFactory[+N <: ColorContext](factory: SelectionListFactory, context: N)
	extends ContextualComponentFactory[N, ColorContext, ContextualSelectionListFactory]
{
	override def withContext[N2 <: ColorContext](newContext: N2) = copy(context = newContext)
	
	/**
	  * Creates a new list
	  * @param contentPointer A pointer to the selection options displayed on this list
	  * @param valuePointer A pointer to the currently selected value (default = new empty pointer)
	  * @param direction Direction along which the selection items are laid (default = Y = vertical)
	  * @param layout Stack layout used for determining display breadth (default = Fit = all have same breadth)
	  * @param margin Margin placed between selectable items (default = determined by context)
	  * @param cap Cap placed at both ends of this list (default = always 0)
	  * @param sameItemCheck A function for testing whether two items should be considered equal
	  *                      (specify only if equals method should <b>not</b> be used) (default = None)
	  * @param makeDisplay A function for creating a new display component. Accepts parent component hierarchy and
	  *                    the initially displayed item.
	  * @tparam A Type of displayed / selected value
	  * @tparam C Type of display component
	  * @tparam P Type of selection pool pointer
	  * @return A new list
	  */
	def apply[A, C <: ReachComponentLike with Refreshable[A], P <: Changing[Vector[A]]]
	(contentPointer: P, valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	 direction: Axis2D = Y, layout: StackLayout = Fit, margin: StackLength = context.stackMargin,
	 cap: StackLength = StackLength.fixedZero,  highlightModifier: Double = 1.0,
	 sameItemCheck: Option[EqualsFunction[A]] = None, alternativeKeyCondition: => Boolean = false)
	(makeDisplay: (ComponentHierarchy, A) => C) =
		factory(context.actorHandler, Fixed(context.background), contentPointer, valuePointer, direction,
			layout, margin, cap, highlightModifier, sameItemCheck, alternativeKeyCondition)(makeDisplay)
}

/**
  * A stack-based list which displays a set of selectable items and manages selection
  * @author Mikko Hilpinen
  * @since 19.12.2020, v0.1
  */
class SelectionList[A, C <: ReachComponentLike with Refreshable[A], +P <: Changing[Vector[A]]]
(parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler, contextBackgroundPointer: View[Color],
 override val contentPointer: P, override val valuePointer: PointerWithEvents[Option[A]], direction: Axis2D,
 layout: StackLayout, margin: StackLength, cap: StackLength, highlightModifier: Double,
 sameItemCheck: Option[EqualsFunction[A]], alternativeKeyCondition: => Boolean)
(makeDisplay: (ComponentHierarchy, A) => C)
	extends ReachComponentWrapper with MutableCustomDrawableWrapper with MutableFocusable
		with SelectionWithPointers[Option[A], PointerWithEvents[Option[A]], Vector[A], P] with CursorDefining
{
	// ATTRIBUTES	---------------------------------
	
	private implicit val canvas: ReachCanvas = parentHierarchy.top
	
	override lazy val focusId = hashCode()
	private val focusTracker = new FocusStateTracker(false)
	override var focusListeners: Seq[FocusListener] = Vector(focusTracker)
	
	private val stack = MutableStack(parentHierarchy)[C](direction, layout, margin, cap)
	private val manager = sameItemCheck match {
		case Some(check) => ContainerSingleSelectionManager.forImmutableStates(stack, contentPointer,
			valuePointer)(check) { item => Open { makeDisplay(_, item) } }
		case None => ContainerSingleSelectionManager.forStatelessItems(stack, contentPointer, valuePointer) { item =>
			Open { makeDisplay(_, item) } }
	}
	
	/**
	  * A pointer that contains the currently selected sub-area within this list.
	  * The origin (0,0) coordinates of the contained bounds are the position of this list.
	  */
	lazy val selectedAreaPointer = manager.selectedDisplayPointer.flatMap { _.headOption match {
		case Some(display) =>
			display.boundsPointer.map { b =>
				stack.areaOf(display).filter { _.size.dimensions.forall { _ > 0.0 } }.orElse { Some(b) }
			}
		case None => Fixed(None)
	} }
	
	private val keyListener = SelectionKeyListener
		.along(direction, hasFocus || alternativeKeyCondition)(manager.moveSelection)
	private val repaintAreaListener: ChangeListener[Option[Bounds]] = e => {
		Bounds.aroundOption(e.values.flatten).foreach { area => repaintArea(area.enlarged(direction(margin.optimal)), High) }
		true
	}
	
	
	// INITIAL CODE	--------------------------------
	
	addHierarchyListener { isAttached =>
		if (isAttached) {
			GlobalKeyboardEventHandler += keyListener
			GlobalMouseEventHandler += GlobalMouseReleaseListener
			actorHandler += keyListener
			addCustomDrawer(SelectionDrawer)
			selectedAreaPointer.addListener(repaintAreaListener)
			SelectionDrawer.hoverAreaPointer.addListener(repaintAreaListener)
			canvas.cursorManager.foreach { _ += this }
			enableFocusHandling()
		}
		else {
			GlobalKeyboardEventHandler -= keyListener
			GlobalMouseEventHandler -= GlobalMouseReleaseListener
			actorHandler -= keyListener
			removeCustomDrawer(SelectionDrawer)
			selectedAreaPointer.removeListener(repaintAreaListener)
			SelectionDrawer.hoverAreaPointer.removeListener(repaintAreaListener)
			canvas.cursorManager.foreach { _ -= this }
			disableFocusHandling()
		}
	}
	
	// Repaints selected area when focus changes
	focusPointer.addContinuousAnyChangeListener { selectedAreaPointer.value.foreach { repaintArea(_) } }
	
	// Listens to local mouse events
	addMouseButtonListener(LocalMouseListener)
	addMouseMoveListener(LocalMouseListener)
	
	
	// COMPUTED	------------------------------------
	
	/**
	  * @return Whether this list is completely empty
	  */
	def isEmpty = content.isEmpty
	
	/**
	  * @return Whether this list contains selectable items
	  */
	def nonEmpty = content.nonEmpty
	
	/**
	  * @return A pointer to this list's focus state
	  */
	def focusPointer = focusTracker.focusPointer
	
	/**
	  * @return Whether this list has currently focus
	  */
	def hasFocus = focusPointer.value
	
	/**
	  * @return The currently selected item display
	  */
	def selectedDisplay = manager.selectedDisplay.headOption
	
	
	// IMPLEMENTED	--------------------------------
	
	// Focus may enter if there are items to select
	override def allowsFocusEnter = nonEmpty
	
	override def allowsFocusLeave = true
	
	override protected def drawable: MutableCustomDrawable = stack
	
	override protected def wrapped: ReachComponent = stack
	
	override def repaint() = super[MutableCustomDrawableWrapper].repaint()
	
	override def cursorType = if (isEmpty) Default else Interactive
	
	override def cursorBounds = boundsInsideTop
	
	override def cursorToImage(cursor: Cursor, position: Point) =
		cursor(ColorShade.forLuminosity(contextBackgroundPointer.value.luminosity))
	
	
	// NESTED	------------------------------------
	
	private object LocalMouseListener extends MouseMoveListener with MouseButtonStateListener
	{
		// ATTRIBUTES	----------------------------
		
		private val relativeMousePositionPointer = new PointerWithEvents[Option[Point]](None)
		val hoverComponentPointer = relativeMousePositionPointer.map { _.flatMap(stack.itemNearestTo) }
		
		private var pressedDisplay: Option[C] = None
		
		// Only listens to left mouse button presses which haven't been consumed yet
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter &&
			Consumable.notConsumedFilter
		
		
		// COMPUTED	--------------------------------
		
		def relativeMousePosition = relativeMousePositionPointer.value
		
		def currentDisplayUnderCursor = hoverComponentPointer.value
		
		def isPressed = pressedDisplay.isDefined
		
		
		// IMPLEMENTED	----------------------------
		
		override def onMouseMove(event: MouseMoveEvent) =
		{
			if (event.isOverArea(bounds))
				relativeMousePositionPointer.value = Some(event.mousePosition - position)
			else
				relativeMousePositionPointer.value = None
		}
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			if (!hasFocus)
				requestFocus()
			pressedDisplay = hoverComponentPointer.value
			SelectionDrawer.hoverAreaPointer.value.foreach { repaintArea(_, High) }
			pressedDisplay.map { d => ConsumeEvent(s"Pressed display $d") }
		}
		
		// Only listens to mouse presses while the mouse is over this component
		override def allowsHandlingFrom(handlerType: HandlerType) = handlerType match {
			case MouseButtonStateHandlerType => relativeMousePosition.isDefined
			case _ => true
		}
		
		
		// OTHER	---------------------------------
		
		def release() =
		{
			val result = pressedDisplay.filter(currentDisplayUnderCursor.contains).map { d =>
				manager.selectDisplay(d)
				ConsumeEvent(s"Selected $d")
			}
			pressedDisplay = None
			SelectionDrawer.hoverAreaPointer.value.foreach { repaintArea(_) }
			result
		}
	}
	
	private object GlobalMouseReleaseListener extends MouseButtonStateListener with Handleable
	{
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftReleasedFilter
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
			LocalMouseListener.release()
	}
	
	private object SelectionDrawer extends CustomDrawer
	{
		// ATTRIBUTES	---------------------------
		
		val hoverAreaPointer = LocalMouseListener.hoverComponentPointer.map { _.flatMap(stack.areaOf) }
		
		
		// IMPLEMENTED	---------------------------
		
		override def opaque = false
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) = {
			lazy val bg = contextBackgroundPointer.value
			def draw(pointer: View[Option[Bounds]], highlightLevel: Double) =
				pointer.value.foreach { area =>
					drawer.draw(area + bounds.position)(DrawSettings.onlyFill(bg.highlightedBy(highlightLevel)))
				}
			
			// Checks whether currently selected area and the mouse area overlap
			if (manager.selectedDisplay.exists(LocalMouseListener.currentDisplayUnderCursor.contains))
				draw(selectedAreaPointer, if (LocalMouseListener.isPressed) 4 else if (hasFocus) 3 else 1)
			else {
				draw(selectedAreaPointer, if (hasFocus) 2 else 1)
				draw(hoverAreaPointer, if (LocalMouseListener.isPressed) 3 else 1)
			}
		}
	}
}
