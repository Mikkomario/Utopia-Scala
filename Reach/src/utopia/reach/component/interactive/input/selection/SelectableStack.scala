package utopia.reach.component.interactive.input.selection

import utopia.firmament.component.input.{InputWithPointer, SelectionWithPointers}
import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.context.color.VariableColorContextLike
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.controller.StackItemAreas
import utopia.firmament.controller.data.SelectionKeyListener
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.Focused
import utopia.firmament.model.enumeration.MouseInteractionState.{Hover, Pressed}
import utopia.firmament.model.enumeration.SizeCategory.Small
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.util.Mutate
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed, LazilyInitializedChanging}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.Background
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer}
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.keyboard.Key.{LeftArrow, RightArrow, UpArrow}
import utopia.genesis.handling.event.keyboard.{Key, KeyboardEvents}
import utopia.genesis.handling.event.mouse._
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.{ContextualMixed, FocusListenableFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reach.component.template.{HasGuiState, PartOfComponentHierarchy, ReachComponent, ReachComponentWrapper}
import utopia.reach.container.multi.{SegmentGroup, ViewStack, ViewStackSettings, ViewStackSettingsLike}
import utopia.reach.focus.{FocusListener, FocusStateTracker}

/**
 * Common trait for selectable stack factories and settings
 * @tparam Repr Implementing factory/settings type
 * @author Mikko Hilpinen
 * @since 12.09.2025, v1.7
 */
trait SelectableStackSettingsLike[+Repr] extends FocusListenableFactory[Repr] with ViewStackSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	 * Settings that affect this stack's layout
	 */
	def stackSettings: ViewStackSettings
	
	/**
	 * A drawer used for visualizing selection and mouse interaction
	 */
	def selectionDrawer: Option[SelectionDrawer]
	/**
	 * A pointer that contains the margin placed between the components in this stack.
	 * May be defined as either a general size category -pointer (left), or a specific length
	 * -pointer (right).
	 */
	def marginPointer: Either[Changing[SizeCategory], Changing[StackLength]]
	/**
	 * Keys (other than the arrow keys), which are used for moving the selection around.
	 * Each key is mapped to the direction to which it moves the selection.
	 */
	def extraSelectionKeys: Map[Key, Sign]
	/**
	 * A flag that, when set, makes the keyboard-based selection function even when the component
	 * doesn't have focus
	 */
	def alternativeKeySelectionEnabledFlag: Flag
	/**
	 * Whether arrow key -based selection should be enabled
	 */
	def arrowKeySelectionEnabled: Boolean
	
	/**
	 * A pointer that contains the margin placed between the components in this stack.
	 * May be defined as either a general size category -pointer (left), or a specific length
	 * -pointer (right).
	 * @param p New margin pointer to use.
	 *          A pointer that contains the margin placed between the components in this stack.
	 *          May be defined as either a general size category -pointer (left), or a specific
	 *          length -pointer (right).
	 * @return Copy of this factory with the specified margin pointer
	 */
	def withMarginPointer(p: Either[Changing[SizeCategory], Changing[StackLength]]): Repr
	/**
	 * Changes the keyboard keys used for moving the selection around.
	 * Note: Overrides previously used keys, including the default arrow keys.
	 * @param keys Keys which are used for moving the selection around.
	 *             Each key is mapped to the direction to which it moves the selection.
	 * @return Copy of this factory with the specified keys used for selection (exclusively)
	 */
	def withSelectionKeys(keys: Map[Key, Sign]): Repr
	/**
	 * Keys (other than the arrow keys), which are used for moving the selection around.
	 * Each key is mapped to the direction to which it moves the selection.
	 * @param keys New additional selection keys to use.
	 *             Keys (other than the arrow keys), which are used for moving the selection around.
	 *             Each key is mapped to the direction to which it moves the selection.
	 * @return Copy of this factory with the specified additional selection keys
	 */
	def withExtraSelectionKeys(keys: Map[Key, Sign]): Repr
	/**
	 * A flag that, when set, makes the keyboard-based selection function even when the component
	 * doesn't have focus
	 * @param enabledFlag New alternative key selection enabled flag to use.
	 *                    A flag that, when set, makes the keyboard-based selection function even
	 *                    when the component doesn't have focus
	 * @return Copy of this factory with the specified alternative key selection enabled flag
	 */
	def withAlternativeKeySelectionEnabledFlag(enabledFlag: Flag): Repr
	/**
	 * Whether arrow key -based selection should be enabled
	 * @param enabled New arrow key selection enabled to use.
	 *                Whether arrow key -based selection should be enabled
	 * @return Copy of this factory with the specified arrow key selection enabled
	 */
	def withArrowKeySelectionEnabled(enabled: Boolean): Repr
	/**
	 * A drawer used for visualizing selection and mouse interaction
	 * @param drawer New selection drawer to use.
	 *               A drawer used for visualizing selection and mouse interaction
	 * @return Copy of this factory with the specified selection drawer
	 */
	def withSelectionDrawer(drawer: Option[SelectionDrawer]): Repr
	
	/**
	 * Settings that affect this stack's layout
	 * @param settings New stack settings to use.
	 *                 Settings that affect this stack's layout
	 * @return Copy of this factory with the specified stack settings
	 */
	def withStackSettings(settings: ViewStackSettings): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Copy of this factory without a selection drawer.
	 *         Useful when the generated components visualize their selection state independently.
	 */
	def withoutSelectionDrawer = withSelectionDrawer(None)
	
	/**
	 * @return Copy of this factory where the created stacks react to keyboard events even while they're not in focus.
	 */
	def withoutFocusKeyRequirement = withAlternativeKeySelectionEnabledFlag(AlwaysTrue)
	
	
	// IMPLEMENTED	--------------------
	
	override def axisPointer: Changing[Axis2D] = stackSettings.axisPointer
	override def layoutPointer: Changing[StackLayout] = stackSettings.layoutPointer
	override def segmentGroup: Option[SegmentGroup] = stackSettings.segmentGroup
	override def capPointer: Changing[StackLength] = stackSettings.capPointer
	override def customDrawers = stackSettings.customDrawers
	
	override def withAxisPointer(p: Changing[Axis2D]): Repr = mapStackSettings { _.withAxisPointer(p) }
	override def withLayoutPointer(p: Changing[StackLayout]): Repr = mapStackSettings { _.withLayoutPointer(p) }
	override def withSegmentGroup(group: Option[SegmentGroup]): Repr = mapStackSettings { _.withSegmentGroup(group) }
	override def withCapPointer(p: Changing[StackLength]): Repr = mapStackSettings { _.withCapPointer(p) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = mapStackSettings { _.withCustomDrawers(drawers) }
	
	
	// OTHER	--------------------
	
	/**
	 * A drawer used for visualizing selection and mouse interaction
	 * @param drawer New selection drawer to use.
	 *               A drawer used for visualizing selection and mouse interaction
	 * @return Copy of this factory with the specified selection drawer
	 */
	def withSelectionDrawer(drawer: SelectionDrawer): Repr = withSelectionDrawer(Some(drawer))
	
	/**
	 * Changes the keyboard keys used for moving the selection around.
	 * Note: Overrides previously used keys, including the default arrow keys.
	 * @param backKey Key used for moving the selection backward
	 * @param forwardKey Key used for moving the selection forward
	 * @return Copy of this factory with the specified keys used for selection (exclusively)
	 */
	def withSelectionKeys(backKey: Key, forwardKey: Key): Repr =
		withSelectionKeys(Map(backKey -> Negative, forwardKey -> Positive))
	
	def withAdditionalSelectionKeys(backKey: Key, forwardKey: Key): Repr =
		withAdditionalSelectionKeys(Map(backKey -> Negative, forwardKey -> Positive))
	def withAdditionalSelectionKeys(keys: Map[Key, Sign]): Repr = mapExtraSelectionKeys { _ ++ keys }
	
	/**
	 * @param backKey Key used for moving the selection backward
	 * @param forwardKey Key used for moving the selection forward
	 * @param exclusive Whether these should be the only keys used for moving the selection (default = false)
	 * @return Copy of this factory with the specified selection keys
	 */
	def movingSelectionUsing(backKey: Key, forwardKey: Key, exclusive: Boolean = false) = {
		if (exclusive)
			withSelectionKeys(backKey, forwardKey)
		else
			withAdditionalSelectionKeys(backKey, forwardKey)
	}
	
	def mapExtraSelectionKeys(f: Mutate[Map[Key, Sign]]) = withExtraSelectionKeys(f(extraSelectionKeys))
	def mapAlternativeKeySelectionEnabledFlag(f: Mutate[Flag]) =
		withAlternativeKeySelectionEnabledFlag(f(alternativeKeySelectionEnabledFlag))
	
	def mapStackSettings(f: Mutate[ViewStackSettings]) = withStackSettings(f(stackSettings))
}

object SelectableStackSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
 * Combined settings used when constructing selectable stacks
 *
 * @param focusListeners                     Focus listeners to assign to created components
 * @param stackSettings                      Settings that affect this stack's layout
 * @param selectionDrawer                    A drawer used for visualizing selection and mouse
 *                                           interaction
 * @param marginPointer                      A pointer that contains the margin placed between
 *                                           the components in this stack.
 *                                           May be defined as either a general size category -pointer (left),
 *                                           or a specific length -pointer (right).
 * @param extraSelectionKeys            Keys (other than the arrow keys), which are used
 *                                           for moving the selection around.
 *                                           Each key is mapped to the direction to which it
 *                                           moves the selection.
 * @param alternativeKeySelectionEnabledFlag A flag that, when set, makes the keyboard-based
 *                                           selection function even when the component doesn't
 *                                           have focus
 * @param arrowKeySelectionEnabled           Whether arrow key -based selection should be enabled
 * @author Mikko Hilpinen
 * @since 12.09.2025, v1.7
 */
case class SelectableStackSettings(focusListeners: Seq[FocusListener] = Empty,
                                   stackSettings: ViewStackSettings = ViewStackSettings.default,
                                   selectionDrawer: Option[SelectionDrawer] = None,
                                   marginPointer: Either[Changing[SizeCategory], Changing[StackLength]] = Left(Fixed(Small)),
                                   extraSelectionKeys: Map[Key, Sign] = Map[Key, Sign](),
                                   alternativeKeySelectionEnabledFlag: Flag = AlwaysFalse,
                                   arrowKeySelectionEnabled: Boolean = true)
	extends SelectableStackSettingsLike[SelectableStackSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withExtraSelectionKeys(keys: Map[Key, Sign]) =
		copy(extraSelectionKeys = keys)
	override def withAlternativeKeySelectionEnabledFlag(enabledFlag: Flag) =
		copy(alternativeKeySelectionEnabledFlag = enabledFlag)
	override def withArrowKeySelectionEnabled(enabled: Boolean) =
		copy(arrowKeySelectionEnabled = enabled)
	override def withFocusListeners(listeners: Seq[FocusListener]) =
		copy(focusListeners = listeners)
	override def withSelectionDrawer(drawer: Option[SelectionDrawer]) =
		copy(selectionDrawer = drawer)
	override def withMarginPointer(p: Either[Changing[SizeCategory], Changing[StackLength]]) =
		copy(marginPointer = p)
	override def withStackSettings(settings: ViewStackSettings) = copy(stackSettings = settings)
	
	override def withSelectionKeys(keys: Map[Key, Sign]): SelectableStackSettings =
		copy(extraSelectionKeys = keys, arrowKeySelectionEnabled = false)
}

/**
 * Common trait for factories that wrap a selectable stack settings instance
 * @tparam Repr Implementing factory/settings type
 * @author Mikko Hilpinen
 * @since 12.09.2025, v1.7
 */
trait SelectableStackSettingsWrapper[+Repr] extends SelectableStackSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	 * Settings wrapped by this instance
	 */
	protected def settings: SelectableStackSettings
	/**
	 * @return Copy of this factory with the specified settings
	 */
	def withSettings(settings: SelectableStackSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def extraSelectionKeys = settings.extraSelectionKeys
	override def alternativeKeySelectionEnabledFlag = settings.alternativeKeySelectionEnabledFlag
	override def arrowKeySelectionEnabled = settings.arrowKeySelectionEnabled
	override def focusListeners = settings.focusListeners
	override def selectionDrawer = settings.selectionDrawer
	override def stackSettings = settings.stackSettings
	override def marginPointer: Either[Changing[SizeCategory], Changing[StackLength]] = settings.marginPointer
	
	override def withExtraSelectionKeys(keys: Map[Key, Sign]) =
		mapSettings { _.withExtraSelectionKeys(keys) }
	override def withAlternativeKeySelectionEnabledFlag(enabledFlag: Flag) =
		mapSettings { _.withAlternativeKeySelectionEnabledFlag(enabledFlag) }
	override def withArrowKeySelectionEnabled(enabled: Boolean) =
		mapSettings { _.withArrowKeySelectionEnabled(enabled) }
	override def withFocusListeners(listeners: Seq[FocusListener]) =
		mapSettings { _.withFocusListeners(listeners) }
	override def withSelectionDrawer(drawer: Option[SelectionDrawer]) =
		mapSettings { _.withSelectionDrawer(drawer) }
	override def withStackSettings(settings: ViewStackSettings) = mapSettings { _.withStackSettings(settings) }
	override def withMarginPointer(p: Either[Changing[SizeCategory], Changing[StackLength]]): Repr =
		mapSettings { _.withMarginPointer(p) }
	
	override def withSelectionKeys(keys: Map[Key, Sign]): Repr = mapSettings { _.withSelectionKeys(keys) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: SelectableStackSettings => SelectableStackSettings) = withSettings(f(settings))
}

/**
 * Factory class used for constructing selectable stacks using contextual component creation
 * information
 * @author Mikko Hilpinen
 * @since 12.09.2025, v1.7
 */
case class SelectableStackFactory[N <: VariableColorContextLike[N, _]](hierarchy: ComponentHierarchy, context: N,
                                                                       settings: SelectableStackSettings = SelectableStackSettings.default)
	extends SelectableStackSettingsWrapper[SelectableStackFactory[N]] with PartOfComponentHierarchy
		with ContextualSelectionFactory[N, SelectableStackFactory[N]]
{
	// IMPLEMENTED	--------------------
	
	override def withSettings(settings: SelectableStackSettings) = copy(settings = settings)
	
	
	// IMPLICIT ------------------------
	
	private implicit def log: Logger = ComponentCreationDefaults.componentLogger
	
	
	// OTHER    ------------------------
	
	def withContext[N2 <: VariableColorContextLike[N2, _]](newContext: N2): SelectableStackFactory[N2] =
		copy(context = newContext)
	def mapContext[N2 <: VariableColorContextLike[N2, _]](f: N => N2) =
		withContext(f(context))
	
	/**
	 * Creates a new selectable stack
	 * @param contentPointer A pointer that contains the displayed content
	 * @param valuePointer A mutable pointer that contains the selected value (default = new pointer)
	 * @param makeView A function that receives:
	 *                      1. Component factory (a [[ContextualMixed]])
	 *                      1. Content pointer
	 *                      1. Selection flag
	 *                      1. Component index (0-based)
	 *
	 *                 And yields a component to display at that position
	 * @param eq Implicit equals-function used for comparing selection. Default = Use ==
	 * @tparam A Type of displayed & selected values
	 * @return A new selectable stack component
	 */
	def apply[A](contentPointer: Changing[Seq[A]], valuePointer: EventfulPointer[Option[A]] = Pointer.eventful.empty[A])
	            (makeView: (ContextualMixed[N], Changing[A], Flag, Int) => ReachComponent)
	            (implicit eq: EqualsFunction[A] = EqualsFunction.default) =
		new SelectableStack[A, N](hierarchy, context, settings, contentPointer, valuePointer, makeView)
}

/**
 * Used for defining selectable stack creation settings outside the component building process
 * @author Mikko Hilpinen
 * @since 12.09.2025, v1.7
 */
case class SelectableStackSetup(settings: SelectableStackSettings = SelectableStackSettings.default)
	extends SelectableStackSettingsWrapper[SelectableStackSetup]
		// Only extends Ccff because the generic version requires a self-referencing type
		with Ccff[VariableTextContext, SelectableStackFactory[VariableTextContext]]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy,
	                         context: VariableTextContext): SelectableStackFactory[VariableTextContext] =
		withContext[VariableTextContext](hierarchy, context)
	
	override def withSettings(settings: SelectableStackSettings) = copy(settings = settings)
	
	
	// OTHER    -------------------------
	
	def withContext[N <: VariableColorContextLike[N, _]](hierarchy: ComponentHierarchy, context: N) =
		SelectableStackFactory(hierarchy, context, settings)
}

object SelectableStack extends SelectableStackSetup()
{
	// OTHER	--------------------
	
	def apply(settings: SelectableStackSettings) = withSettings(settings)
}

/**
 * Adds selection features to a view stack
 * @tparam A Type of selected items
 * @tparam N Type of used component creation context
 * @author Mikko Hilpinen
 * @since 09.09.2025, v1.7
 */
class SelectableStack[A, N <: VariableColorContextLike[N, _]](override val hierarchy: ComponentHierarchy, context: N,
                                                              settings: SelectableStackSettings,
                                                              override val contentPointer: Changing[Seq[A]],
                                                              override val valuePointer: EventfulPointer[Option[A]],
                                                              makeView: (ContextualMixed[N], Changing[A], Flag, Int) => ReachComponent)
                                                             (implicit eq: EqualsFunction[A])
	extends ReachComponentWrapper with FocusableWithState with HasGuiState
		with SelectionWithPointers[Option[A], EventfulPointer[Option[A]], Seq[A], Changing[Seq[A]]]
		with InputWithPointer[Option[A], EventfulPointer[Option[A]]]
{
	// ATTRIBUTES   ---------------------------
	
	override lazy val focusId: Int = hashCode()
	private val focusTracker = new FocusStateTracker()
	override lazy val focusListeners: Seq[FocusListener] = focusTracker +: settings.focusListeners
	override val allowsFocusLeave: Boolean = true
	
	private val stateP = Pointer.eventful(GuiElementStatus.identity)
	
	private lazy val selectedIndexP = valuePointer.mergeWith(contentPointer) { (selected, content) =>
		selected.flatMap { selected => content.findIndexWhere { eq(selected, _) } }
	}
	
	override protected lazy val wrapped = {
		val baseF = ViewStack.withContext(hierarchy, context).withSettings(settings.stackSettings)
			.withCustomDrawer(settings.selectionDrawer match {
				case Some(drawer) => new LocalSelectionDrawer(drawer)
				case None => new HoverDrawer
			})
		val stackF = settings.marginPointer match {
			case Left(sizeP) => baseF.withMarginSizePointer(sizeP)
			case Right(marginP) => baseF.withMarginPointer(marginP)
		}
		stackF.mapPointer(contentPointer, Mixed) { (factory, pointer, index) =>
			// Tracks selection status and modifies the background pointer accordingly
			val (selectedFlag, correctBgFactory) = settings.selectionDrawer
				.flatMap { _.selectionBackgroundPointer } match
			{
				// Case: Selected item background is different from normal => Creates a custom background color -pointer
				case Some(bgP) =>
					val selectedFlag: Flag = selectedIndexP.lightMap { _.contains(index) }
					val modifiedFactory = factory.mapContext { _.mapBackgroundPointer { original =>
						selectedFlag.flatMap { if (_) bgP else original }
					} }
					
					selectedFlag -> modifiedFactory
				
				// Case: Selected components have the same background
				case None =>
					LazilyInitializedChanging { selectedIndexP.lightMap { _.contains(index) } } -> factory
			}
			makeView(correctBgFactory, pointer, selectedFlag, index)
		}
	}
	
	private lazy val locationTracker = new StackItemAreas[ReachComponent](wrapped, componentsP)
	
	private val listensToKeyboard = settings.arrowKeySelectionEnabled || settings.extraSelectionKeys.nonEmpty
	
	/**
	 * A change listener for repainting changed selection and/or hover areas
	 */
	private val areaChangeListener = ChangeListener[Option[Bounds]] { e =>
		e.values.flatten.reduceOption { (prev, now) => Bounds.around(Pair(prev, now)) }.foreach { repaintArea(_) }
	}
	
	
	// INITIAL CODE -----------------------
	
	// Updates the state as necessary
	focusFlag.addListener { e => if (e.newValue) stateP.update { _ + Focused } else stateP.update { _ - Focused } }
	MouseHoverTracker.hoverFlag.addListener { e =>
		if (e.newValue) stateP.update { _ + Hover } else stateP.update { _ - Hover }
	}
	MousePressListener.pressedFlag.addListener { e =>
		if (e.newValue) stateP.update { _ + Pressed } else stateP.update { _ - Pressed }
	}
	
	linkedFlag.onceSet {
		// Sets up mouse listening
		mouseMoveHandler += MouseHoverTracker
		mouseButtonHandler += MousePressListener
		CommonMouseEvents += MouseReleaseListener
		
		// Sets up key listening
		if (listensToKeyboard) {
			val keysP = settings.axisPointer.map { axis =>
				val axisKeys: Map[Key, Sign] = axis match {
					case X => Map(LeftArrow -> Negative, RightArrow -> Positive)
					case Y => Map(UpArrow -> Negative, RightArrow -> Positive)
				}
				axisKeys ++ settings.extraSelectionKeys
			}
			val listener = SelectionKeyListener
				.withEnabledFlag((linkedFlag && wrapped.visibleFlag) &&
					(focusFlag || settings.alternativeKeySelectionEnabledFlag))
				.listeningTo(keysP)
				.apply(moveSelectionBy)
			
			context.actorHandler += listener
			KeyboardEvents += listener
		}
	}
	
	
	// COMPUTED ---------------------------
	
	def selected_=(value: Option[A]) = valuePointer.value = value
	def selected_=(value: A) = valuePointer.setOne(value)
	
	private def selectedIndex = selectedIndexP.value
	
	private def componentsP = wrapped.componentsPointer
	private def components = wrapped.components
	
	
	// IMPLEMENTED  -----------------------
	
	override def allowsFocusEnter: Boolean = listensToKeyboard && components.nonEmpty
	override def focusFlag: Flag = focusTracker.focusFlag
	
	override def state: GuiElementStatus = stateP.value
	
	
	// OTHER    ---------------------------
	
	/**
	 * Adjusts the selection by the specified amount
	 * @param adjustment Number of items to traverse, in terms of selection
	 */
	def moveSelectionBy(adjustment: Int): Unit = {
		if (adjustment != 0) {
			val currentContent = content
			val optionsCount = currentContent.size
			if (optionsCount > 0)
				selectedIndex match {
					// Case: An item was selected => Adjusts the selection
					case Some(previous) =>
						var index = previous + adjustment
						while (index < 0) {
							index += optionsCount
						}
						currentContent.lift(index % optionsCount).foreach { valuePointer.setOne(_) }
					
					// Case: No items selected yet => Selects the first or the last item
					case None =>
						if (adjustment > 0)
							valuePointer.setOne(currentContent.head)
						else
							valuePointer.setOne(currentContent.last)
				}
		}
	}
	/**
	 * Moves the selection to the next available item
	 */
	def selectNext() = moveSelectionBy(1)
	/**
	 * Moves the selection to the previous available item
	 */
	def selectPrevious() = moveSelectionBy(-1)
	
	
	// NESTED   ---------------------------
	
	/**
	 * Tracks mouse hover state and location
	 */
	private object MouseHoverTracker extends MouseMoveListener
	{
		// ATTRIBUTES   -------------------
		
		override lazy val mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		
		/**
		 * Contains the last mouse position over this component, relative to this component's top left corner
		 */
		private val relativeMouseLocationP = Pointer.eventful.empty[Point]
		/**
		 * Contains true while the mouse is hovering over this component
		 */
		lazy val hoverFlag: Flag = relativeMouseLocationP.lightMap { _.isDefined }
		
		/**
		 * Contains the area of the component being hovered over
		 */
		lazy val hoverOverAreaP = relativeMouseLocationP.map { _.flatMap(locationTracker.areaNearestTo) }
		
		
		// IMPLEMENTED  -------------------
		
		// Only tracks hover while there are components in this stack
		override def handleCondition: Flag = wrapped.visibleFlag
		
		override def onMouseMove(event: MouseMoveEvent): Unit = {
			// Updates the hover state & position
			if (event.isOver(bounds))
				relativeMouseLocationP.setOne(event.position.relative - position)
			else
				relativeMouseLocationP.clear()
		}
	}
	/**
	 * Listens to mouse presses inside this component, setting the "pressed" state
	 */
	private object MousePressListener extends MouseButtonStateListener
	{
		// ATTRIBUTES   ------------------------
		
		// Listens to mouse presses over this component
		override lazy val handleCondition: Flag = wrapped.visibleFlag && MouseHoverTracker.hoverFlag
		override lazy val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] =
			MouseButtonStateEvent.filter.leftPressed
		
		/**
		 * Contains the component that was pressed. None if no component was pressed.
		 */
		val pressedComponentP = Pointer.eventful.empty[ReachComponent]
		/**
		 * Contains true while in a "pressed" state
		 */
		val pressedFlag: Flag = pressedComponentP.lightMap { _.isDefined }
		
		
		// IMPLEMENTED  ------------------------
		
		// Sets the "pressed" state
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice =
			pressedComponentP.value = locationTracker.itemNearestTo(event.position.relative - position)
	}
	/**
	 * Listens to mouse releases anywhere.
	 * Used for resetting the "pressed" state.
	 */
	private object MouseReleaseListener extends MouseButtonStateListener
	{
		// ATTRIBUTES   ---------------------------
		
		// Only listens while in the pressed state. Also, won't listen while not attached to the component hierarchy.
		override lazy val handleCondition: Flag = linkedFlag && wrapped.visibleFlag && MousePressListener.pressedFlag
		override lazy val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] =
			MouseButtonStateEvent.filter.leftReleased
			
		
		// IMPLEMENTED  ---------------------------
		
		// When mouse is released over the same component it was pressed on, selects that component's value
		// Otherwise just ends the "pressed" state
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice =
			MousePressListener.pressedComponentP.pop().filter { c => event.relativeTo(position).isOver(c.bounds) }
				.flatMap(components.findIndexOf).flatMap(content.lift).foreach { selected = _ }
	}
	
	/**
	 * Visualizes selection & hover states using a selection drawer
	 * @param wrapped The wrapped selection drawer
	 */
	private class LocalSelectionDrawer(wrapped: SelectionDrawer) extends CustomDrawer
	{
		// ATTRIBUTES   -------------------
		
		override val opaque: Boolean = false
		
		private val selectedAreaP = selectedIndexP.mergeWith(componentsP) { (index, components) =>
			index.flatMap(components.lift).flatMap(locationTracker.areaOf)
		}
		
		
		// INITIAL CODE -------------------
		
		// Repaints this component when selected area changes
		selectedAreaP.addListenerWhile(linkedFlag)(areaChangeListener)
		// Also repaints when hover area changes
		MouseHoverTracker.hoverOverAreaP.addListenerWhile(linkedFlag)(areaChangeListener)
		
		
		// IMPLEMENTED  -------------------
		
		override def drawLevel: DrawLevel = wrapped.drawLevel
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = {
			val selectedArea = selectedAreaP.value
			// Visualizes hover, if distinct from the selected area
			MouseHoverTracker.hoverOverAreaP.value.filterNot { area => selectedArea.exists { _ ~== area } }
				.foreach { hoverArea =>
					wrapped.draw(drawer, bounds, hoverArea, mouseInteractionLevel,
						hasFocus = hasFocus, selected = false)
				}
			// Visualizes selection
			selectedArea.foreach { selected =>
				wrapped.draw(drawer, bounds, selected, mouseInteractionLevel, hasFocus = hasFocus, selected = true)
			}
		}
	}
	/**
	 * Used for visualizing the mouse hover state when no selection drawing is used
	 */
	private class HoverDrawer extends CustomDrawer
	{
		// ATTRIBUTES   --------------------------
		
		override val opaque: Boolean = false
		override val drawLevel: DrawLevel = Background
		
		
		// INITIAL CODE --------------------------
		
		MouseHoverTracker.hoverOverAreaP.addListenerWhile(linkedFlag)(areaChangeListener)
		
		
		// IMPLEMENTED  -------------------------
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = MouseHoverTracker.hoverOverAreaP.value
			.foreach { hoverArea =>
				implicit val ds: DrawSettings = DrawSettings.onlyFill(
					context.backgroundPointer.value.highlightedBy(mouseInteractionLevel.level))
				drawer.draw(hoverArea + bounds.position)
			}
	}
}
