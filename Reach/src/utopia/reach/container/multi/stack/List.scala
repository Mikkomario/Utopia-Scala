package utopia.reach.container.multi.stack

import utopia.flow.datastructure.immutable.Lazy
import utopia.flow.datastructure.mutable.{Pointer, PointerWithEvents, Settable}
import utopia.flow.datastructure.template.{LazyLike, Viewable}
import utopia.flow.event.{ChangingLike, Fixed}
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.event.{Consumable, ConsumeEvent, KeyStateEvent, MouseButtonStateEvent, MouseEvent, MouseMoveEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener, MouseMoveListener}
import utopia.genesis.shape.shape2D.Direction2D.{Down, Up}
import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.genesis.util.Drawer
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.{ComponentHierarchy, SeedHierarchyBlock}
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.template.focus.Focusable
import utopia.reach.component.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reach.container.ReachCanvas
import utopia.reach.focus.{FocusListener, FocusStateTracker}
import utopia.reflection.color.ComponentColor
import utopia.reflection.component.context.ColorContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.shape.stack.{StackLength, StackSize}

import java.awt.event.KeyEvent

case class ListRowContext(parentHierarchy: SeedHierarchyBlock, selectionPointer: LazyLike[ChangingLike[Boolean]],
						  rowIndex: Int)

case class ListRowContent(components: IterableOnce[ReachComponentLike], context: ListRowContext, action: () => Unit)

/**
  * Used for creating actionable lists
  * @author Mikko Hilpinen
  * @since 12.12.2020, v0.1
  */
object List extends ContextInsertableComponentFactoryFactory[ColorContextLike, ListFactory, ContextualListFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ListFactory(hierarchy)
}

class ListFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContextLike, ContextualListFactory]
{
	private implicit val canvas: ReachCanvas = parentHierarchy.top
	
	override def withContext[N <: ColorContextLike](context: N) =
		ContextualListFactory(this, context)
	
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
	def apply[R](group: SegmentGroup, contextBackgroundPointer: ChangingLike[ComponentColor],
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
		val selectedRowIndexPointer = new PointerWithEvents[Int](0)
		val rowIndexGenerator = Iterator.iterate(0) { _ + 1 }
		val rowContextIterator = rowIndexGenerator.map { index =>
			ListRowContext(new SeedHierarchyBlock(canvas), Lazy { selectedRowIndexPointer.map { _ == index } }, index)
		}
		val (content, result) = fill(rowContextIterator).toTuple
		val mainStackContent = Open.using(Stack) { rowF =>
			content.iterator.map { rowContent =>
				val wrappedRowComponents = new OpenComponent(rowContent.components.iterator.toVector,
					rowContent.context.parentHierarchy)
				
				(rowF(wrappedRowComponents, rowDirection, insideRowLayout, columnMargin,
					rowCap).parent: ReachComponentLike) -> rowContent
			}.splitMap { p => p }
		}
		val rowsWithResults = mainStackContent.component.zip(mainStackContent.result)
		val rowsWithIndices = rowsWithResults.map { case (c, r) => c -> r.context.rowIndex }
		
		// Then creates the main stack
		val selectedComponentPointer = selectedRowIndexPointer
			.map { i => rowsWithIndices.find { _._2 == i }.map { _._1 } }
		val keyPressedPointer = new Pointer(false)
		val stackPointer = new Pointer[Option[Stack[ReachComponentLike]]](None)
		val selector = new Selector(stackPointer, contextBackgroundPointer, selectedComponentPointer, keyPressedPointer)
		val stackCreation = Stack(parentHierarchy)(mainStackContent, rowDirection.perpendicular, Fit, rowMargin,
			mainStackCap, selector +: customDrawers)
		if (rowsWithResults.nonEmpty)
		{
			val stack = stackCreation.parent
			stackPointer.value = Some(stack)
			stack.addMouseMoveListener(selector)
			stack.addMouseButtonListener(selector)
			// Also adds item selection on left mouse press
			stack.addMouseButtonListener(MouseButtonStateListener(MouseButtonStateEvent.leftPressedFilter &&
				Consumable.notConsumedFilter && MouseEvent.isOverAreaFilter(stack.bounds)) { e =>
				stack.itemNearestTo(e.mousePosition - stack.position).flatMap { c => rowsWithIndices.find { _._1 == c } }
					.map { case (_, index) =>
						selectedRowIndexPointer.value = index
						ConsumeEvent("List item selected")
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
					GlobalKeyboardEventHandler += keyListener
				else
					GlobalKeyboardEventHandler -= keyListener
			}
		}
		
		stackCreation.withResult(result)
	}
}

case class ContextualListFactory[+N <: ColorContextLike](factory: ListFactory, context: N)
	extends ContextualComponentFactory[N, ColorContextLike, ContextualListFactory]
{
	override def withContext[N2 <: ColorContextLike](newContext: N2) =
		copy(context = newContext)
	
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
		factory(group, Fixed(context.containerBackground), insideRowLayout, context.defaultStackMargin,
			context.relatedItemsStackMargin, edgeMargins, customDrawers, focusListeners)(fill)
}

private class SelectionKeyListener(selectedIndexPointer: Settable[Int], keyPressedPointer: Settable[Boolean], maxIndex: Int,
								   focusStatePointer: Viewable[Boolean], actions: Map[Int, () => Unit])
	extends KeyStateListener
{
	private val triggerKeys = Set(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE)
	
	def selectedIndex = selectedIndexPointer.value
	def selectedIndex_=(newValue: Int) = selectedIndexPointer.value = newValue
	
	def pressed = keyPressedPointer.value
	def pressed_=(newState: Boolean) = keyPressedPointer.value = newState
	
	override def onKeyState(event: KeyStateEvent) =
	{
		if (event.isDown)
		{
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
		else if (triggerKeys.contains(event.index) && pressed)
		{
			// Triggers the action
			actions.get(selectedIndex).foreach { _() }
			pressed = false
		}
	}
	
	// Only listens to events while has focus
	override def allowsHandlingFrom(handlerType: HandlerType) = focusStatePointer.value
}

private class Selector(stackPointer: Viewable[Option[Stack[ReachComponentLike]]],
					   backgroundPointer: Viewable[ComponentColor],
					   selectedComponentPointer: ChangingLike[Option[ReachComponentLike]],
					   keyPressedPointer: Viewable[Boolean])
	extends CustomDrawer with MouseMoveListener with MouseButtonStateListener with Handleable
{
	// ATTRIBUTES	----------------------------------
	
	private val selectedAreaPointer = selectedComponentPointer
		.lazyMap { c => stack.flatMap { s => c.flatMap(s.areaOf) } }
	
	private var mousePressed = false
	private val relativeMousePositionPointer = new PointerWithEvents[Option[Point]](None)
	private val mouseOverAreaPointer = relativeMousePositionPointer
		.lazyMap { p => stack.flatMap { s => p.flatMap(s.areaNearestTo) } }
	
	override val mouseButtonStateEventFilter = Consumable.notConsumedFilter
	
	
	// COMPUTED	-------------------------------------
	
	def stack = stackPointer.value
	def relativeMousePosition = relativeMousePositionPointer.value
	def selectedArea = selectedAreaPointer.value
	def mouseOverArea = mouseOverAreaPointer.value
	def defaultSelectionHighlight = if (keyPressedPointer.value) 0.225 else 0.15
	
	
	// IMPLEMENTED	----------------------------------
	
	override def opaque = false
	
	override def drawLevel = Normal
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		def draw(highlightLevel: Double, area: Bounds) =
			drawer.onlyFill(backgroundPointer.value.highlightedBy(highlightLevel)).draw(area)
		def drawMouseArea() = mouseOverArea.foreach { draw(if (mousePressed) 0.225 else 0.075, _) }
		
		relativeMousePosition match
		{
			case Some(mousePosition) =>
				selectedArea match
				{
					case Some(selectedArea) =>
						// Case: Mouse is over the selected area
						if (selectedArea.contains(mousePosition))
							draw(if (mousePressed) 0.225 else defaultSelectionHighlight, selectedArea)
						// Case: Mouse area and selected area are separate
						else
						{
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
	
	override def onMouseMove(event: MouseMoveEvent) =
	{
		stack.foreach { stack =>
			if (event.isOverArea(stack.bounds))
				relativeMousePositionPointer.value = Some(event.mousePosition - stack.position)
			else if (relativeMousePosition.nonEmpty)
			{
				relativeMousePositionPointer.value = None
				mousePressed = false
			}
		}
	}
	
	override def onMouseButtonState(event: MouseButtonStateEvent) =
	{
		if (event.wasReleased)
			mousePressed = false
		// else if (stack.exists { s => event.isOverArea(s.bounds) })
		else if (relativeMousePosition.isDefined)
			mousePressed = true
		None
	}
}