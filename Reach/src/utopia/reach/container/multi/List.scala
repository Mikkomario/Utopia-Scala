package utopia.reach.container.multi

import utopia.firmament.context.ColorContext
import utopia.firmament.controller.StackItemAreas
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.{EventfulPointer, SettableOnce}
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.event.consume.Consumable
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.KeyStateListener.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener, MouseEvent, MouseMoveEvent, MouseMoveListener}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextFactory
import utopia.reach.component.factory.contextual.ColorContextualFactory
import utopia.reach.component.hierarchy.{ComponentHierarchy, SeedHierarchyBlock}
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.template.focus.Focusable
import utopia.reach.component.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reach.container.ReachCanvas
import utopia.reach.focus.{FocusListener, FocusStateTracker}

import java.awt.event.KeyEvent

case class ListRowContext(parentHierarchy: SeedHierarchyBlock, selectionPointer: Lazy[Changing[Boolean]],
                          rowIndex: Int)

case class ListRowContent(components: IterableOnce[ReachComponentLike], context: ListRowContext, action: () => Unit)

/**
  * Used for creating actionable lists
  * @author Mikko Hilpinen
  * @since 12.12.2020, v0.1
  */
object List extends Cff[ListFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ListFactory(hierarchy)
}

class ListFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[ColorContext, ContextualListFactory]
{
	private implicit val canvas: ReachCanvas = parentHierarchy.top
	
	override def withContext(context: ColorContext) = ContextualListFactory(this, context)
	
	/**
	  * Creates a new list with content
	  * @param group Group that determines column alignment
	  * @param contextBackgroundPointer A pointer to the background color of the parent container
	  * @param insideRowLayout Layout to use for the column segments inside rows (default = Fit)
	  * @param rowMargin Margin placed between rows (default = any, preferring 0)
	  * @param columnMargin Margin placed between columns (default = any, preferring 0)
	  * @param edgeMargins Margins placed at the outer edges of this list (default = always 0)
	  * @param customDrawers Custom drawers assigned to this list (default = empty)
	  * @param focusListeners Focus listeners assigned to this list (default = empty)
	  * @param fill A function that accepts an infinite iterator of contexts for new rows and produces the desirable
	  *             amount of row content, along with a possible additional result
	  * @tparam R Type of additional result created
	  * @return A new list (wrap result)
	  */
	// TODO: Refactor using settings and variable context
	// TODO: Look for overlap with Collection
	def apply[R](group: SegmentGroup, contextBackgroundPointer: Changing[Color],
	             insideRowLayout: StackLayout = Fit, rowMargin: StackLength = StackLength.any,
	             columnMargin: StackLength = StackLength.any, edgeMargins: StackSize = StackSize.fixedZero,
	             customDrawers: Vector[CustomDrawer] = Vector(), focusListeners: Seq[FocusListener] = Vector())
				(fill: Iterator[ListRowContext] => ComponentCreationResult[IterableOnce[ListRowContent], R]) =
	{
		val rowDirection = group.rowDirection
		val rowCap = edgeMargins.along(rowDirection)
		val mainStackDirection = rowDirection.perpendicular
		val mainStackCap = edgeMargins.along(mainStackDirection)
		
		// Creates the rows and row components first
		val selectedRowIndexPointer = new EventfulPointer[Int](0)
		val rowIndexGenerator = Iterator.iterate(0) { _ + 1 }
		val rowContextIterator = rowIndexGenerator.map { index =>
			ListRowContext(new SeedHierarchyBlock(canvas), Lazy { selectedRowIndexPointer.map { _ == index } }, index)
		}
		val (content, result) = fill(rowContextIterator).toTuple
		val mainStackContent = Open.using(Stack) { rowF =>
			content.iterator.map { rowContent =>
				val wrappedRowComponents = new OpenComponent(rowContent.components.iterator.toVector,
					rowContent.context.parentHierarchy)
				val row = rowF.withAxis(rowDirection).withLayout(insideRowLayout).withMargin(columnMargin)
					.withCap(rowCap)(wrappedRowComponents)
					.parent: ReachComponentLike
				row -> rowContent
			}.splitMap { p => p }
		}
		val rowsWithResults = mainStackContent.component.zip(mainStackContent.result)
		val rowsWithIndices = rowsWithResults.map { case (c, r) => c -> r.context.rowIndex }
		
		// Then creates the main stack
		val selectedComponentPointer = selectedRowIndexPointer
			.map { i => rowsWithIndices.find { _._2 == i }.map { _._1 } }
		val keyPressedPointer = Pointer(false)
		val stackPointer = SettableOnce[Stack]()
		val selector = new Selector(stackPointer, contextBackgroundPointer, selectedComponentPointer, keyPressedPointer)
		val stackCreation = Stack(parentHierarchy).withAxis(rowDirection.perpendicular).withMargin(rowMargin)
			.withCap(mainStackCap).withCustomDrawers(selector +: customDrawers)(mainStackContent)
		if (rowsWithResults.nonEmpty) {
			val stack = stackCreation.parent
			stackPointer.value = Some(stack)
			stack += selector
			// Also adds item selection on left mouse press
			val locations = StackItemAreas(stack)
			stack.addMouseButtonListener(MouseButtonStateListener
				.filtering(MouseButtonStateEvent.filter.leftPressed && Consumable.unconsumedFilter &&
					MouseEvent.filter.over(stack.bounds))
				{ e =>
					locations.itemNearestTo(e.position - stack.position)
						.flatMap { c => rowsWithIndices.find { _._1 == c } } match
					{
						case Some((_, index)) =>
							selectedRowIndexPointer.value = index
							Consume("List item selected")
						case None => Preserve
					}
				})
			// And keyboard listening for selected index changing
			val maxRowIndex = rowsWithIndices.map { _._2 }.max
			val focusTracker = new FocusStateTracker(false)
			Focusable.wrap(stack, focusTracker +: focusListeners)
			val keyListener = new SelectionKeyListener(selectedRowIndexPointer, keyPressedPointer, maxRowIndex,
				focusTracker.focusPointer,
				rowsWithResults.map { case (_, result) => result.context.rowIndex -> result.action }.toMap)
			stack.addHierarchyListener { isAttached =>
				if (isAttached)
					KeyboardEvents += keyListener
				else
					KeyboardEvents -= keyListener
			}
		}
		
		stackCreation.withResult(result)
	}
}

case class ContextualListFactory(factory: ListFactory, context: ColorContext)
	extends ColorContextualFactory[ContextualListFactory]
{
	override def self: ContextualListFactory = this
	
	override def withContext(newContext: ColorContext) = copy(context = newContext)
	
	/**
	  * Creates a new list with content
	  * @param group Group that determines column alignment
	  * @param insideRowLayout Layout to use for the column segments inside rows (default = Fit)
	  * @param edgeMargins Margins placed at the outer edges of this list (default = always 0)
	  * @param customDrawers Custom drawers assigned to this list (default = empty)
	  * @param focusListeners Focus listeners assigned to this list (default = empty)
	  * @param fill A function that accepts an infinite iterator of contexts for new rows and produces the desirable
	  *             amount of row content, along with a possible additional result
	  * @tparam R Type of additional result created
	  * @return A new list (wrap result)
	  */
	def apply[R](group: SegmentGroup, insideRowLayout: StackLayout = Fit, edgeMargins: StackSize = StackSize.fixedZero,
				 customDrawers: Vector[CustomDrawer] = Vector(), focusListeners: Seq[FocusListener] = Vector())
				(fill: Iterator[ListRowContext] => ComponentCreationResult[IterableOnce[ListRowContent], R]) =
		factory(group, Fixed(context.background), insideRowLayout, context.stackMargin,
			context.smallStackMargin, edgeMargins, customDrawers, focusListeners)(fill)
}

private object SelectionKeyListener
{
	private val triggerKeys = Set(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE)
}
private class SelectionKeyListener(selectedIndexPointer: Pointer[Int], keyPressedPointer: Pointer[Boolean], maxIndex: Int,
                                   focusPointer: FlagLike, actions: Map[Int, () => Unit])
	extends KeyStateListener
{
	import SelectionKeyListener._
	
	
	// COMPUTED -----------------------
	
	def selectedIndex = selectedIndexPointer.value
	def selectedIndex_=(newValue: Int) = selectedIndexPointer.value = newValue
	
	def pressed = keyPressedPointer.value
	def pressed_=(newState: Boolean) = keyPressedPointer.value = newState
	
	
	// IMPLEMENTED  -------------------
	
	// Only listens to events while has focus
	override def handleCondition: FlagLike = focusPointer
	override def keyStateEventFilter: KeyStateEventFilter = AcceptAll
	
	override def onKeyState(event: KeyStateEvent) = {
		if (event.pressed) {
			// Checks for trigger keys
			if (triggerKeys.contains(event.index))
				pressed = true
			else
				// Checks for up/down keys
				event.verticalArrow.foreach {
					case Up => if (selectedIndex > 0) selectedIndex -= 1 else selectedIndex = maxIndex
					case Down => if (selectedIndex < maxIndex - 1) selectedIndex += 1 else selectedIndex = 0
					case _ => ()
				}
		}
		else if (triggerKeys.contains(event.index) && pressed) {
			// Triggers the action
			actions.get(selectedIndex).foreach { _() }
			pressed = false
		}
	}
}

private class Selector(stackPointer: Changing[Option[Stack]], backgroundPointer: View[Color],
                       selectedComponentPointer: Changing[Option[ReachComponentLike]],
                       keyPressedPointer: View[Boolean])
	extends CustomDrawer with MouseMoveListener with MouseButtonStateListener
{
	// ATTRIBUTES	----------------------------------
	
	private lazy val locationTrackerPointer = stackPointer.map { _.map { StackItemAreas(_) } }
	private lazy val selectedAreaPointer = locationTrackerPointer
		.lazyMergeWith(selectedComponentPointer) { (items, selected) =>
			items.flatMap { items => selected.flatMap(items.areaOf) }
		}
	
	private var mousePressed = false
	
	private val relativeMousePositionPointer = new EventfulPointer[Option[Point]](None)
	private lazy val mouseOverAreaPointer = relativeMousePositionPointer
		.lazyMergeWith(locationTrackerPointer) { (pos, items) =>
			pos.flatMap { pos => items.flatMap { _.areaNearestTo(pos) } }
		}
	
	override val mouseButtonStateEventFilter = Consumable.unconsumedFilter
	
	
	// COMPUTED	-------------------------------------
	
	def stack = stackPointer.value
	def relativeMousePosition = relativeMousePositionPointer.value
	def selectedArea = selectedAreaPointer.value
	def mouseOverArea = mouseOverAreaPointer.value
	def defaultSelectionHighlight = if (keyPressedPointer.value) 2 else 1
	
	
	// IMPLEMENTED	----------------------------------
	
	override def opaque = false
	override def drawLevel = Normal
	
	override def handleCondition: FlagLike = AlwaysTrue
	override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
	
	override def draw(drawer: Drawer, bounds: Bounds) = {
		def draw(highlightLevel: Double, area: Bounds) =
			drawer.draw(area)(DrawSettings.onlyFill(backgroundPointer.value.highlightedBy(highlightLevel)))
		def drawMouseArea() = mouseOverArea.foreach { draw(if (mousePressed) 0.225 else 0.075, _) }
		
		relativeMousePosition match {
			case Some(mousePosition) =>
				selectedArea match {
					case Some(selectedArea) =>
						// Case: Mouse is over the selected area
						if (selectedArea.contains(mousePosition))
							draw(if (mousePressed) 2 else defaultSelectionHighlight, selectedArea)
						// Case: Mouse area and selected area are separate
						else {
							draw(defaultSelectionHighlight, selectedArea)
							drawMouseArea()
						}
					// Case: There is no selected area
					case None => drawMouseArea()
				}
			// Case: No mouse over
			case None => selectedArea.foreach { draw(defaultSelectionHighlight, _) }
		}
	}
	
	override def onMouseMove(event: MouseMoveEvent) = {
		stack.foreach { stack =>
			if (event.isOver(stack.bounds))
				relativeMousePositionPointer.value = Some(event.position - stack.position)
			else if (relativeMousePosition.nonEmpty) {
				relativeMousePositionPointer.value = None
				mousePressed = false
			}
		}
	}
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
		if (event.released)
			mousePressed = false
		// else if (stack.exists { s => event.isOverArea(s.bounds) })
		else if (relativeMousePosition.isDefined)
			mousePressed = true
	}
}