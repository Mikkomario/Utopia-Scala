package utopia.reach.component.input.selection

import utopia.firmament.component.display.Refreshable
import utopia.firmament.component.input.SelectionWithPointers
import utopia.firmament.context.color.VariableColorContext
import utopia.firmament.controller.StackItemAreas
import utopia.firmament.controller.data.{ContainerSingleSelectionManager, SelectionKeyListener}
import utopia.firmament.drawing.mutable.{MutableCustomDrawable, MutableCustomDrawableWrapper}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.Single
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.Priority.High
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.consume.Consumable
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.KeyboardEvents
import utopia.genesis.handling.event.mouse.{CommonMouseEvents, MouseButtonStateEvent, MouseButtonStateListener, MouseMoveEvent, MouseMoveListener}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.factory.contextual.ContextualFactory
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.focus.MutableFocusable
import utopia.reach.component.template.{CursorDefining, PartOfComponentHierarchy, ReachComponent, ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.Open
import utopia.reach.container.multi.{MutableStack, StackSettings, StackSettingsLike}
import utopia.reach.cursor.Cursor
import utopia.reach.cursor.CursorType.{Default, Interactive}
import utopia.reach.focus.{FocusListener, FocusStateTracker}

/**
  * Common trait for selection list factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait SelectionListSettingsLike[+Repr] extends StackSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings that affect the stack layout of this list
	  */
	def stackSettings: StackSettings
	/**
	  * A modifier that is applied to the color highlighting used in this component.
	  * 1.0 signifies the default color highlighting.
	  */
	def highlightModifier: Double
	
	/**
	  * A modifier that is applied to the color highlighting used in this component.
	  * 1.0 signifies the default color highlighting.
	  * @param modifier New highlight modifier to use.
	  *                 A modifier that is applied to the color highlighting used in this component.
	  *                 1.0 signifies the default color highlighting.
	  * @return Copy of this factory with the specified highlight modifier
	  */
	def withHighlightModifier(modifier: Double): Repr
	/**
	  * Settings that affect the stack layout of this list
	  * @param settings New stack settings to use.
	  *                 Settings that affect the stack layout of this list
	  * @return Copy of this factory with the specified stack settings
	  */
	def withStackSettings(settings: StackSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def axis = stackSettings.axis
	override def capPointer: Changing[StackLength] = stackSettings.capPointer
	override def customDrawers = stackSettings.customDrawers
	override def layout = stackSettings.layout
	
	override def withAxis(axis: Axis2D) = withStackSettings(stackSettings.withAxis(axis))
	override def withCapPointer(p: Changing[StackLength]): Repr = mapStackSettings { _.withCapPointer(p) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) =
		withStackSettings(stackSettings.withCustomDrawers(drawers))
	override def withLayout(layout: StackLayout) = withStackSettings(stackSettings.withLayout(layout))
	
	
	// OTHER	--------------------
	
	def mapHighlightModifier(f: Double => Double) = withHighlightModifier(f(highlightModifier))
	def mapStackSettings(f: StackSettings => StackSettings) = withStackSettings(f(stackSettings))
}

object SelectionListSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
  * Combined settings used when constructing selection lists
  * @param stackSettings     Settings that affect the stack layout of this list
  * @param highlightModifier A modifier that is applied to the color highlighting used in this component.
  *                          1.0 signifies the default color highlighting.
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class SelectionListSettings(stackSettings: StackSettings = StackSettings.default,
                                 highlightModifier: Double = 1.0)
	extends SelectionListSettingsLike[SelectionListSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withHighlightModifier(modifier: Double) = copy(highlightModifier = modifier)
	override def withStackSettings(settings: StackSettings) = copy(stackSettings = settings)
}

/**
  * Common trait for factories that wrap a selection list settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait SelectionListSettingsWrapper[+Repr] extends SelectionListSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: SelectionListSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: SelectionListSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def highlightModifier = settings.highlightModifier
	override def stackSettings = settings.stackSettings
	
	override def withHighlightModifier(modifier: Double) = mapSettings { _.withHighlightModifier(modifier) }
	override def withStackSettings(settings: StackSettings) = mapSettings { _.withStackSettings(settings) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: SelectionListSettings => SelectionListSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing selection lists
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait SelectionListFactoryLike[+Repr] extends SelectionListSettingsWrapper[Repr] with PartOfComponentHierarchy
{
	import utopia.firmament.context.ComponentCreationDefaults.componentLogger
	
	// ABSTRACT	--------------------
	
	/**
	  * @return Pointer that determines the margin placed between selectable items
	  */
	protected def marginPointer: Changing[StackLength]
	/**
	  * @param p A pointer that determines the size of margins placed between selectable items
	  * @return Copy of this factory with the specified margins pointer
	  */
	def withMarginPointer(p: Changing[StackLength]): Repr
	
	
	// COMPUTED -------------------
	
	/**
	  * @return Copy of this factory that doesn't place any margin between the selectable items
	  */
	def withoutMargin = withMargin(StackLength.fixedZero)
	
	
	// OTHER    -------------------
	
	/**
	  * @param margin Margin to place between the items in the stacks
	  * @return Copy of this factory with specified margin
	  */
	def withMargin(margin: StackLength) = withMarginPointer(Fixed(margin))
	
	/**
	  * Creates a new list
	  * @param actorHandler             Actor handler that will deliver action events for arrow key handling
	  * @param contextBackgroundPointer A pointer to the background color of this list's container / context
	  * @param contentPointer           A pointer to the selection options displayed on this list
	  * @param valuePointer             A pointer to the currently selected value (default = new empty pointer)
	  * @param sameItemCheck            A function for testing whether two items should be considered equal
	  *                                 (specify only if equals method should <b>not</b> be used) (default = None)
	  * @param alternativeKeyCondition  A function that returns true in cases where selection key events should be
	  *                                 enabled. Key events are always enabled while this list is in focus.
	  *                                 Default = false = Key events are received only while in focus.
	  * @param makeDisplay              A function for creating a new display component. Accepts parent component hierarchy and
	  *                                 the initially displayed item.
	  * @tparam A Type of displayed / selected value
	  * @tparam C Type of display component
	  * @tparam P Type of selection pool pointer
	  * @return A new list
	  */
	protected def _apply[A, C <: ReachComponentLike with Refreshable[A], P <: Changing[Seq[A]]]
	(actorHandler: ActorHandler, contextBackgroundPointer: View[Color], contentPointer: P,
	 valuePointer: EventfulPointer[Option[A]] = EventfulPointer[Option[A]](None),
	 sameItemCheck: Option[EqualsFunction[A]] = None, alternativeKeyCondition: => Boolean = false)
	(makeDisplay: (ComponentHierarchy, A) => C) =
		new SelectionList[A, C, P](parentHierarchy, actorHandler, contextBackgroundPointer, contentPointer,
			valuePointer, settings, marginPointer, sameItemCheck, alternativeKeyCondition)(makeDisplay)
}

/**
  * Factory class that is used for constructing selection lists without using contextual information
  * @param marginPointer A pointer that determines the size of margins placed between selectable items
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class SelectionListFactory(parentHierarchy: ComponentHierarchy,
                                settings: SelectionListSettings = SelectionListSettings.default,
                                marginPointer: Changing[StackLength] = Fixed(StackLength.any))
	extends SelectionListFactoryLike[SelectionListFactory]
		with FromContextFactory[VariableColorContext, ContextualSelectionListFactory]
{
	import utopia.firmament.context.ComponentCreationDefaults.componentLogger
	
	// IMPLEMENTED  ----------------------
	
	override def withContext(c: VariableColorContext): ContextualSelectionListFactory =
		ContextualSelectionListFactory(parentHierarchy, c, settings)
	
	override def withSettings(settings: SelectionListSettings) = copy(settings = settings)
	def withMarginPointer(p: Changing[StackLength]) = copy(marginPointer = p)
	
	
	// OTHER    -------------------------
	
	/**
	  * Creates a new list
	  * @param actorHandler             Actor handler that will deliver action events for arrow key handling
	  * @param contextBackgroundPointer A pointer to the background color of this list's container / context
	  * @param contentPointer           A pointer to the selection options displayed on this list
	  * @param valuePointer             A pointer to the currently selected value (default = new empty pointer)
	  * @param sameItemCheck            A function for testing whether two items should be considered equal
	  *                                 (specify only if equals method should <b>not</b> be used) (default = None)
	  * @param alternativeKeyCondition  A function that returns true in cases where selection key events should be
	  *                                 enabled. Key events are always enabled while this list is in focus.
	  *                                 Default = false = Key events are received only while in focus.
	  * @param makeDisplay              A function for creating a new display component. Accepts parent component hierarchy and
	  *                                 the initially displayed item.
	  * @tparam A Type of displayed / selected value
	  * @tparam C Type of display component
	  * @tparam P Type of selection pool pointer
	  * @return A new list
	  */
	def apply[A, C <: ReachComponentLike with Refreshable[A], P <: Changing[Seq[A]]]
	(actorHandler: ActorHandler, contextBackgroundPointer: View[Color], contentPointer: P,
	 valuePointer: EventfulPointer[Option[A]] = EventfulPointer[Option[A]](None),
	 sameItemCheck: Option[EqualsFunction[A]] = None, alternativeKeyCondition: => Boolean = false)
	(makeDisplay: (ComponentHierarchy, A) => C) =
		_apply[A, C, P](actorHandler, contextBackgroundPointer, contentPointer, valuePointer, sameItemCheck,
			alternativeKeyCondition)(makeDisplay)
}

/**
  * Factory class used for constructing selection lists using contextual component creation information
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class ContextualSelectionListFactory(parentHierarchy: ComponentHierarchy,
                                          context: VariableColorContext,
                                          settings: SelectionListSettings = SelectionListSettings.default,
                                          customMarginPointer: Option[Changing[StackLength]] = None)
	extends SelectionListFactoryLike[ContextualSelectionListFactory]
		with ContextualFactory[VariableColorContext, ContextualSelectionListFactory]
{
	// ATTRIBUTES   -------------------------
	
	override protected lazy val marginPointer: Changing[StackLength] =
		customMarginPointer.getOrElse { context.smallStackMarginPointer }
	
	
	// IMPLEMENTED  -------------------------
	
	override def withContext(context: VariableColorContext) = copy(context = context)
	override def withSettings(settings: SelectionListSettings) = copy(settings = settings)
	override def withMarginPointer(p: Changing[StackLength]) =
		copy(customMarginPointer = Some(p))
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param margin Margin size to use
	  * @return Copy of this factory that places the specified sized margin between list items.
	  */
	def withMargin(margin: SizeCategory) =
		withMarginPointer(context.scaledStackMarginPointer(margin))
	/**
	  * @param margin Margin size to use. None if no margin should be placed.
	  * @return Copy of this factory that uses the specified margin between list items.
	  */
	def withMargin(margin: Option[SizeCategory]): ContextualSelectionListFactory = margin match {
		case Some(m) => withMargin(m)
		case None => withoutMargin
	}
	
	/**
	  * Creates a new list
	  * @param contentPointer A pointer to the selection options displayed on this list
	  * @param valuePointer A pointer to the currently selected value (default = new empty pointer)
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
	def apply[A, C <: ReachComponentLike with Refreshable[A], P <: Changing[Seq[A]]]
	(contentPointer: P, valuePointer: EventfulPointer[Option[A]] = EventfulPointer[Option[A]](None),
	 sameItemCheck: Option[EqualsFunction[A]] = None, alternativeKeyCondition: => Boolean = false)
	(makeDisplay: (ComponentHierarchy, A) => C) =
		_apply[A, C, P](context.actorHandler, context.backgroundPointer, contentPointer,
			valuePointer, sameItemCheck, alternativeKeyCondition)(makeDisplay)
}

/**
  * Used for defining selection list creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class SelectionListSetup(settings: SelectionListSettings = SelectionListSettings.default)
	extends SelectionListSettingsWrapper[SelectionListSetup]
		with ComponentFactoryFactory[SelectionListFactory]
		with FromContextComponentFactoryFactory[VariableColorContext, ContextualSelectionListFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = SelectionListFactory(hierarchy, settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableColorContext): ContextualSelectionListFactory =
		ContextualSelectionListFactory(hierarchy, context, settings)
	override def withSettings(settings: SelectionListSettings) = copy(settings = settings)
}

object SelectionList extends SelectionListSetup()
{
	// OTHER	--------------------
	
	def apply(settings: SelectionListSettings) = withSettings(settings)
}

/**
  * A stack-based list which displays a set of selectable items and manages selection
  * @author Mikko Hilpinen
  * @since 19.12.2020, v0.1
  */
class SelectionList[A, C <: ReachComponentLike with Refreshable[A], +P <: Changing[Seq[A]]]
(parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler, contextBackgroundPointer: View[Color],
 override val contentPointer: P, override val valuePointer: EventfulPointer[Option[A]],
 settings: SelectionListSettings = SelectionListSettings.default,
 marginPointer: Changing[StackLength] = Fixed(StackLength.any), sameItemCheck: Option[EqualsFunction[A]],
 alternativeKeyCondition: => Boolean)
(makeDisplay: (ComponentHierarchy, A) => C)
	extends ReachComponentWrapper with MutableCustomDrawableWrapper with MutableFocusable
		with SelectionWithPointers[Option[A], EventfulPointer[Option[A]], Seq[A], P] with CursorDefining
{
	// ATTRIBUTES	---------------------------------
	
	override lazy val focusId = hashCode()
	private val focusTracker = new FocusStateTracker(false)
	override var focusListeners: Seq[FocusListener] = Single(focusTracker)
	
	private val stack = MutableStack(parentHierarchy)
		.withSettings(settings.stackSettings)
		.withMargin(marginPointer.value)[C]()
	private val locationTracker = new StackItemAreas[C](stack, stack.componentsPointer)
	private val manager = sameItemCheck match {
		case Some(check) =>
			ContainerSingleSelectionManager.forImmutableStates(stack, contentPointer,
				valuePointer)(check) { item => Open { makeDisplay(_, item) } }
		case None =>
			ContainerSingleSelectionManager.forStatelessItems(stack, contentPointer, valuePointer) { item =>
				Open { makeDisplay(_, item) }
			}
	}
	
	/**
	  * A pointer that contains the currently selected sub-area within this list.
	  * The origin (0,0) coordinates of the contained bounds are the position of this list.
	  */
	lazy val selectedAreaPointer = manager.selectedDisplayPointer.flatMap { d => d.headOption match {
		case Some(display) =>
			display.boundsPointer.map { b =>
				locationTracker.areaOf(display).filter { _.size.dimensions.forall { _ > 0.0 } }.orElse { Some(b) }
			}
		case None => Fixed(None)
	} }
	
	private val keyListener = SelectionKeyListener
		.along(settings.axis, hasFocus || alternativeKeyCondition)(manager.moveSelection)
	
	private val repaintAreaListener: ChangeListener[Option[Bounds]] = e => {
		Bounds.aroundOption(e.values.flatten).foreach { area =>
			repaintArea(area.enlarged(settings.axis(marginPointer.value.optimal)), High)
		}
	}
	private val revalidateListener = ChangeListener.continuousOnAnyChange { stack.revalidate() }
	
	
	// INITIAL CODE	--------------------------------
	
	addHierarchyListener { isAttached =>
		if (isAttached) {
			KeyboardEvents += keyListener
			CommonMouseEvents += CommonMouseReleaseListener
			actorHandler += keyListener
			addCustomDrawer(SelectionDrawer)
			marginPointer.addListenerAndSimulateEvent(stack.margin)(revalidateListener)
			selectedAreaPointer.addListener(repaintAreaListener)
			SelectionDrawer.hoverAreaPointer.addListener(repaintAreaListener)
			canvas.cursorManager.foreach { _ += this }
			enableFocusHandling()
		}
		else {
			KeyboardEvents -= keyListener
			CommonMouseEvents -= CommonMouseReleaseListener
			actorHandler -= keyListener
			removeCustomDrawer(SelectionDrawer)
			marginPointer.removeListener(revalidateListener)
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
	
	override protected def wrapped: ReachComponent = stack
	override protected def drawable: MutableCustomDrawable = stack
	
	// Focus may enter if there are items to select
	override def allowsFocusEnter = nonEmpty
	override def allowsFocusLeave = true
	
	override def cursorType = if (isEmpty) Default else Interactive
	override def cursorBounds = boundsInsideTop
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(contextBackgroundPointer.value.shade)
	
	override def repaint() = super[MutableCustomDrawableWrapper].repaint()
	
	
	// NESTED	------------------------------------
	
	private object LocalMouseListener extends MouseMoveListener with MouseButtonStateListener
	{
		// ATTRIBUTES	----------------------------
		
		private val relativeMousePositionPointer = EventfulPointer[Option[Point]](None)
		// FIXME: stack.itemNearestTo doesn't return the correct item anymore
		val hoverComponentPointer = relativeMousePositionPointer.map { _.flatMap(locationTracker.itemNearestTo) }
		
		private var pressedDisplay: Option[C] = None
		
		// Only listens to left mouse button presses which haven't been consumed yet
		override val mouseButtonStateEventFilter =
			MouseButtonStateEvent.filter.leftPressed && Consumable.unconsumedFilter
		
		
		// COMPUTED	--------------------------------
		
		def relativeMousePosition = relativeMousePositionPointer.value
		
		def currentDisplayUnderCursor = hoverComponentPointer.value
		
		def isPressed = pressedDisplay.isDefined
		
		
		// IMPLEMENTED	----------------------------
		
		override def handleCondition: Flag = AlwaysTrue
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		
		override def onMouseMove(event: MouseMoveEvent) = {
			if (event.isOver(bounds))
				relativeMousePositionPointer.value = Some(event.position - position)
			else
				relativeMousePositionPointer.value = None
		}
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			// Only listens to mouse presses while the mouse is over this component
			if (relativeMousePosition.isDefined) {
				if (!hasFocus)
					requestFocus()
				pressedDisplay = hoverComponentPointer.value
				SelectionDrawer.hoverAreaPointer.value.foreach { repaintArea(_, High) }
				pressedDisplay match {
					case Some(display) => Consume(s"Pressed display $display")
					case None => Preserve
				}
			}
			else
				Preserve
		}
		
		
		// OTHER	---------------------------------
		
		def release() = {
			val result = pressedDisplay.filter(currentDisplayUnderCursor.contains) match {
				case Some(display) =>
					manager.selectDisplay(display)
					Consume(s"Selected $display")
				case None => Preserve
			}
			pressedDisplay = None
			SelectionDrawer.hoverAreaPointer.value.foreach { repaintArea(_) }
			result
		}
	}
	
	private object CommonMouseReleaseListener extends MouseButtonStateListener
	{
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.filter.leftReleased
		
		override def handleCondition: Flag = AlwaysTrue
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) =
			LocalMouseListener.release()
	}
	
	private object SelectionDrawer extends CustomDrawer
	{
		// ATTRIBUTES	---------------------------
		
		val hoverAreaPointer = LocalMouseListener.hoverComponentPointer.map { _.flatMap(locationTracker.areaOf) }
		
		
		// IMPLEMENTED	---------------------------
		
		override def opaque = false
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) = {
			lazy val bg = contextBackgroundPointer.value
			def draw(pointer: View[Option[Bounds]], highlightLevel: Double) =
				pointer.value.foreach { area =>
					drawer.draw(area + bounds.position)(
						DrawSettings.onlyFill(bg.highlightedBy(highlightLevel * settings.highlightModifier)))
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
