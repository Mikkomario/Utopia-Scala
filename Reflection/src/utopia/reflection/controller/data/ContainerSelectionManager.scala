package utopia.reflection.controller.data

import utopia.firmament.component.AreaOfItems
import utopia.firmament.component.container.many.MutableMultiContainer
import utopia.firmament.component.display.Refreshable
import utopia.firmament.controller.data.{ContainerContentDisplayer, ContentManager, SelectionKeyListener, SelectionManager}
import utopia.firmament.drawing.mutable.MutableCustomDrawable
import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.Key.{DownArrow, UpArrow}
import utopia.genesis.handling.event.keyboard.{Key, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener, MouseEvent}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.reflection.component.template.ReflectionComponentLike
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.controller.data.ContainerSelectionManager.SelectStack

import scala.concurrent.duration.Duration

object ContainerSelectionManager
{
	private type SelectStack[X <: ReflectionComponentLike] =
		MutableMultiContainer[X, X] with ReflectionStackable with MutableCustomDrawable with AreaOfItems[X]
	
	/**
	  * Creates a content manager for immutable items that don't represent state of any other object. No two different
	  * items will be linked in any way.
	  * @param container Container that will hold the displays
	  * @param selectionAreaDrawer A drawer that will highlight the selected area
	  * @param contentPointer Pointer to the displayed content
	  * @param equalsCheck Function for checking item equality (default = standard equals (== -operator))
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forStatelessItemsPointer[A, Display <: ReflectionStackable with Refreshable[A]]
	(container: SelectStack[Display], selectionAreaDrawer: CustomDrawer, contentPointer: EventfulPointer[Vector[A]],
	 equalsCheck: EqualsFunction[A] = EqualsFunction.default)(makeDisplay: A => Display) =
		new ContainerSelectionManager[A, Display](container, selectionAreaDrawer, contentPointer, equalsCheck)(makeDisplay)
	
	/**
	  * Creates a content manager for immutable items that don't represent state of any other object. No two different
	  * items will be linked in any way.
	  * @param container Container that will hold the displays
	  * @param selectionAreaDrawer A drawer that will highlight the selected area
	  * @param initialItems Initially displayed content (default = empty vector)
	  * @param equalsCheck Function for checking item equality (default = standard equals (== -operator))
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forStatelessItems[A, Display <: ReflectionStackable with Refreshable[A]]
	(container: SelectStack[Display], selectionAreaDrawer: CustomDrawer, initialItems: Vector[A] = Vector(),
	 equalsCheck: EqualsFunction[A] = EqualsFunction.default)(makeDisplay: A => Display) =
		forStatelessItemsPointer[A, Display](container, selectionAreaDrawer,
			EventfulPointer(initialItems), equalsCheck)(makeDisplay)
	
	/**
	  * Creates a content manager for immutable items that represent a state of some other object
	  * (Eg. different immutable states of a single entity). The states may be linked together via a function
	  * (Eg. by checking related database item row id)
	  * @param container Container that will hold the displays
	  * @param selectionAreaDrawer A drawer that will highlight the selected area
	  * @param contentPointer Pointer to the displayed content
	  * @param sameItemCheck Function for checking whether the two items represent the same instance. If you would use
	  *                      a standard equals function (==), please call 'forStatelessItemsPointer' instead since
	  *                      equals function is used for checking display equality.
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forImmutableStatesPointer[A, Display <: ReflectionStackable with Refreshable[A]]
	(container: SelectStack[Display], selectionAreaDrawer: CustomDrawer, contentPointer: EventfulPointer[Vector[A]])(
		sameItemCheck: EqualsFunction[A])(makeDisplay: A => Display) =
		new ContainerSelectionManager[A, Display](container, selectionAreaDrawer, contentPointer, sameItemCheck,
			Some((a: A, b: A) => a == b))(makeDisplay)
	
	/**
	  * Creates a content manager for immutable items that represent a state of some other object
	  * (Eg. different immutable states of a single entity). The states may be linked together via a function
	  * (Eg. by checking related database item row id)
	  * @param container Container that will hold the displays
	  * @param selectionAreaDrawer A drawer that will highlight the selected area
	  * @param initialItems Initially displayed content (default = empty vector)
	  * @param sameItemCheck Function for checking whether the two items represent the same instance. If you would use
	  *                      a standard equals function (==), please call 'forStatelessItems' instead since
	  *                      equals function is used for checking display equality.
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forImmutableStates[A, Display <: ReflectionStackable with Refreshable[A]]
	(container: SelectStack[Display], selectionAreaDrawer: CustomDrawer, initialItems: Vector[A] = Vector())(
		sameItemCheck: EqualsFunction[A])(makeDisplay: A => Display) =
		forImmutableStatesPointer[A, Display](container, selectionAreaDrawer, EventfulPointer(initialItems))(sameItemCheck)(makeDisplay)
	
	/**
	  * Creates a content manager for mutable / mutating items. Please note that the items may not always update
	  * correctly since mutations inside the content do not trigger content change events. Therefore you may manually
	  * need to trigger updates for the container's displays.
	  * @param container Container that will hold the displays
	  * @param selectionAreaDrawer A drawer that will highlight the selected area
	  * @param contentPointer Pointer to the displayed content
	  * @param sameItemCheck Function for checking whether the two items represent the same instance.
	  *                      (Eg. by checking unique id)
	  * @param equalsCheck Function for checking whether the two items are considered completely equal display-wise
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forMutableItemsPointer[A, Display <: ReflectionStackable with Refreshable[A]]
	(container: SelectStack[Display], selectionAreaDrawer: CustomDrawer, contentPointer: EventfulPointer[Vector[A]])(
		sameItemCheck: EqualsFunction[A])(equalsCheck: EqualsFunction[A])(makeDisplay: A => Display) =
		new ContainerSelectionManager[A, Display](container, selectionAreaDrawer, contentPointer, sameItemCheck,
			Some(equalsCheck))(makeDisplay)
	
	/**
	  * Creates a content manager for mutable / mutating items. Please note that the items may not always update
	  * correctly since mutations inside the content do not trigger content change events. Therefore you may manually
	  * need to trigger updates for the container's displays.
	  * @param container Container that will hold the displays
	  * @param selectionAreaDrawer A drawer that will highlight the selected area
	  * @param initialItems Initially displayed content (default = empty vector)
	  * @param sameItemCheck Function for checking whether the two items represent the same instance.
	  *                      (Eg. by checking unique id)
	  * @param equalsCheck Function for checking whether the two items are considered completely equal display-wise
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forMutableItems[A, Display <: ReflectionStackable with Refreshable[A]]
	(container: SelectStack[Display], selectionAreaDrawer: CustomDrawer, initialItems: Vector[A] = Vector())(
		sameItemCheck: EqualsFunction[A])(equalsCheck: EqualsFunction[A])(makeDisplay: A => Display) =
		forMutableItemsPointer[A, Display](container, selectionAreaDrawer, EventfulPointer(initialItems))(
			sameItemCheck)(equalsCheck)(makeDisplay)
}

/**
  * This class handles content and selection in a stack
  * @author Mikko Hilpinen
  * @since 5.6.2019, v1+
  */
class ContainerSelectionManager[A, C <: ReflectionStackable with Refreshable[A]]
(container: SelectStack[C], selectionAreaDrawer: CustomDrawer,
 contentPointer: EventfulPointer[Vector[A]] = EventfulPointer[Vector[A]](Vector()),
 sameItemCheck: EqualsFunction[A] = EqualsFunction.default, equalsCheck: Option[EqualsFunction[A]] = None)(makeItem: A => C)
	extends ContainerContentDisplayer[A, C, C, EventfulPointer[Vector[A]]](container, contentPointer,
		sameItemCheck, equalsCheck)(makeItem) with SelectionManager[A, Option[A], C, EventfulPointer[Vector[A]]]
		with ContentManager[A, C]
{
	// ATTRIBUTES   --------------------
	
	override val valuePointer: EventfulPointer[Option[A]] = EventfulPointer.empty()
	
	// Updates the display value every time content is updated, because the display may change or be not found anymore
	override lazy val selectedDisplayPointer =
		valuePointer.mergeWith(contentPointer) { (selected, _) => selected.flatMap(displayFor) }
	
	
	// INITIAL CODE	--------------------
	
	container.addCustomDrawer(SelectionDrawer)
	selectedDisplayPointer.addContinuousAnyChangeListener { container.repaint() }
	setup()
	
	
	// IMPLEMENTED	--------------------
	
	override protected def itemToSelection(item: A): Option[A] = Some(item)
	
	override protected def itemInSelection(item: A, selection: Option[A]): Option[A] = Some(item)
	
	/*
	override protected def updateSelectionDisplay(oldSelected: Option[C], newSelected: Option[C]) =
		container.repaint()
	*/
	override protected def finalizeRefresh() = {
		super.finalizeRefresh()
		container.repaint()
	}
	
	
	// OTHER	------------------------
	
	/**
	  * Enables mouse button state handling for the stack (selects the clicked item)
	  * @param consumeEvents Whether mouse events should be consumed (default = true)
	  */
	def enableMouseHandling(consumeEvents: Boolean = true) =
		container.addMouseButtonListener(new MouseHandler(consumeEvents))
	/**
	  * Enables key state handling for the stack (allows selection change with up & down arrows)
	  */
	def enableKeyHandling(actorHandler: ActorHandler, nextKey: Key = DownArrow, prevKey: Key = UpArrow,
	                      initialScrollDelay: Duration = 0.4.seconds, scrollDelayModifier: Double = 0.8,
	                      minScrollDelay: Duration = 0.05.seconds,
	                      listenEnabledCondition: => Boolean = true) =
	{
		val listener = new SelectionKeyListener(nextKey, prevKey, listenEnabledCondition,
			initialScrollDelay, scrollDelayModifier, minScrollDelay)(amount => moveSelection(amount))
		container.addStackHierarchyChangeListener(attached => {
			if (attached)
				KeyboardEvents += listener
			else
				KeyboardEvents -= listener
		}, callIfAttached = true)
		actorHandler += listener
	}
	
	
	// NESTED CLASSES	----------------
	
	private object SelectionDrawer extends CustomDrawer
	{
		override def drawLevel = selectionAreaDrawer.drawLevel
		
		override def opaque = false
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			// Draws the selected area using another custom drawer
			selectedDisplay.flatMap(container.areaOf).foreach { area => selectionAreaDrawer.draw(drawer,
				area.translated(bounds.position)) }
		}
	}
	
	private class MouseHandler(val consumeEvents: Boolean) extends MouseButtonStateListener
	{
		// Only considers left mouse button presses inside stack bounds
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.filter.leftPressed &&
			MouseEvent.filter.over(container.bounds)
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			val nearest = container.itemNearestTo(event.position.relative - container.position)
			nearest.foreach(selectDisplay)
			
			if (consumeEvents && nearest.isDefined)
				Consume("Stack selection change")
			else
				Preserve
		}
	}
}
