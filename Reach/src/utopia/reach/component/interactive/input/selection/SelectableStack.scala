package utopia.reach.component.interactive.input.selection

import utopia.firmament.component.input.{InputWithPointer, SelectionWithPointers}
import utopia.firmament.context.color.VariableColorContextLike
import utopia.firmament.controller.StackItemAreas
import utopia.firmament.controller.data.SelectionKeyListener
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.Focused
import utopia.firmament.model.enumeration.MouseInteractionState.{Hover, Pressed}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.view.immutable.eventful.{AlwaysFalse, LazilyInitializedChanging}
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
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.ContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reach.component.template.{HasGuiState, ReachComponent, ReachComponentWrapper}
import utopia.reach.container.multi.{ViewStack, ViewStackSettings}
import utopia.reach.focus.{FocusListener, FocusStateTracker}

/**
 * Adds selection features to a view stack
 * @tparam A Type of selected items
 * @tparam N Type of used component creation context
 * @tparam F Type of component factory used for constructing the wrapped components
 * @author Mikko Hilpinen
 * @since 09.09.2025, v1.7
 */
class SelectableStack[A, N <: VariableColorContextLike[N, _], F <: ContextualFactory[N, F]]
(override val hierarchy: ComponentHierarchy, context: N, stackSettings: ViewStackSettings,
 override val contentPointer: Changing[Seq[A]], override val valuePointer: EventfulPointer[Option[A]],
 viewFactory: Ccff[N, F], makeView: (F, Changing[A], Flag, Int) => ReachComponent,
 selectionDrawer: Option[SelectionDrawer], arrowKeySelectionEnabled: Boolean, otherSelectionKeys: Map[Key, Sign],
 alternativeKeySelectionCondition: Flag = AlwaysFalse, otherFocusListeners: Seq[FocusListener])
(implicit eq: EqualsFunction[A])
	extends ReachComponentWrapper with FocusableWithState with HasGuiState
		with SelectionWithPointers[Option[A], EventfulPointer[Option[A]], Seq[A], Changing[Seq[A]]]
		with InputWithPointer[Option[A], EventfulPointer[Option[A]]]
{
	// ATTRIBUTES   ---------------------------
	
	override lazy val focusId: Int = hashCode()
	private val focusTracker = new FocusStateTracker()
	override lazy val focusListeners: Seq[FocusListener] = focusTracker +: otherFocusListeners
	override val allowsFocusLeave: Boolean = true
	
	private val stateP = Pointer.eventful(GuiElementStatus.identity)
	
	private lazy val selectedIndexP = valuePointer.mergeWith(contentPointer) { (selected, content) =>
		selected.flatMap { selected => content.findIndexWhere { eq(selected, _) } }
	}
	
	override protected lazy val wrapped = ViewStack.withContext(hierarchy, context).withSettings(stackSettings)
		.withCustomDrawer(selectionDrawer match {
			case Some(drawer) => new LocalSelectionDrawer(drawer)
			case None => new HoverDrawer
		})
		.mapPointer(contentPointer, viewFactory) { (factory, pointer, index) =>
			// Tracks selection status and modifies the background pointer accordingly
			val (selectedFlag, correctBgFactory) = selectionDrawer.flatMap { _.selectionBackgroundPointer } match {
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
	
	private lazy val locationTracker = new StackItemAreas[ReachComponent](wrapped, componentsP)
	
	private val listensToKeyboard = arrowKeySelectionEnabled || otherSelectionKeys.nonEmpty
	
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
			val keysP = stackSettings.axisPointer.map { axis =>
				val axisKeys: Map[Key, Sign] = axis match {
					case X => Map(LeftArrow -> Negative, RightArrow -> Positive)
					case Y => Map(UpArrow -> Negative, RightArrow -> Positive)
				}
				axisKeys ++ otherSelectionKeys
			}
			val listener = SelectionKeyListener
				.withEnabledFlag((linkedFlag && wrapped.visibleFlag) && (focusFlag || alternativeKeySelectionCondition))
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
			MouseButtonStateEvent.filter.pressed
		
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
			MouseButtonStateEvent.filter.released
			
		
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
