package utopia.reflection.controller.data

import utopia.firmament.component.AreaOfItems
import utopia.firmament.component.container.many.MutableMultiContainer
import utopia.flow.operator.EqualsFunction
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.event.{ConsumeEvent, MouseButtonStateEvent, MouseEvent}
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.MouseButtonStateListener
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.shape.shape2d.Bounds
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.template.ReflectionComponentLike
import utopia.firmament.component.display.Refreshable
import utopia.firmament.drawing.mutable.MutableCustomDrawable
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.controller.data.ContainerSelectionManager.SelectStack

import java.awt.event.KeyEvent
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
	(container: SelectStack[Display], selectionAreaDrawer: CustomDrawer, contentPointer: PointerWithEvents[Vector[A]],
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
			new PointerWithEvents(initialItems), equalsCheck)(makeDisplay)
	
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
	(container: SelectStack[Display], selectionAreaDrawer: CustomDrawer, contentPointer: PointerWithEvents[Vector[A]])(
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
		forImmutableStatesPointer[A, Display](container, selectionAreaDrawer, new PointerWithEvents(initialItems))(sameItemCheck)(makeDisplay)
	
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
	(container: SelectStack[Display], selectionAreaDrawer: CustomDrawer, contentPointer: PointerWithEvents[Vector[A]])(
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
		forMutableItemsPointer[A, Display](container, selectionAreaDrawer, new PointerWithEvents(initialItems))(
			sameItemCheck)(equalsCheck)(makeDisplay)
}

/**
  * This class handles content and selection in a stack
  * @author Mikko Hilpinen
  * @since 5.6.2019, v1+
  */
class ContainerSelectionManager[A, C <: ReflectionStackable with Refreshable[A]]
(container: SelectStack[C], selectionAreaDrawer: CustomDrawer,
 contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
 sameItemCheck: EqualsFunction[A] = EqualsFunction.default, equalsCheck: Option[EqualsFunction[A]] = None)(makeItem: A => C)
	extends ContainerContentManager[A, MutableMultiContainer[C, C] with ReflectionStackable, C](container, contentPointer,
		sameItemCheck, equalsCheck)(makeItem) with SelectionManager[A, C]
{
	// INITIAL CODE	--------------------
	
	container.addCustomDrawer(SelectionDrawer)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def updateSelectionDisplay(oldSelected: Option[C], newSelected: Option[C]) =
		container.repaint()
	
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
	def enableKeyHandling(actorHandler: ActorHandler, nextKeyCode: Int = KeyEvent.VK_DOWN, prevKeyCode: Int = KeyEvent.VK_UP,
						  initialScrollDelay: Duration = 0.4.seconds, scrollDelayModifier: Double = 0.8,
						  minScrollDelay: Duration = 0.05.seconds,
						  listenEnabledCondition: Option[() => Boolean] = None) =
	{
		val listener = new SelectionKeyListener(nextKeyCode, prevKeyCode, initialScrollDelay, scrollDelayModifier,
			minScrollDelay, listenEnabledCondition)(amount => moveSelection(amount))
		container.addStackHierarchyChangeListener(attached => {
			if (attached)
				GlobalKeyboardEventHandler += listener
			else
				GlobalKeyboardEventHandler -= listener
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
	
	private class MouseHandler(val consumeEvents: Boolean) extends MouseButtonStateListener with Handleable
	{
		// Only considers left mouse button presses inside stack bounds
		override def mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter &&
			MouseEvent.isOverAreaFilter(container.bounds)
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			val nearest = container.itemNearestTo(event.mousePosition - container.position)
			nearest.foreach(handleMouseClick)
			
			if (consumeEvents && nearest.isDefined)
				Some(ConsumeEvent("Stack selection change"))
			else
				None
		}
	}
}
