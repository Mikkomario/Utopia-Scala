package utopia.reflection.controller.data

import java.awt.event.KeyEvent
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.operator.EqualsFunction
import utopia.flow.time.TimeExtensions._
import utopia.genesis.event.{ConsumeEvent, MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.MouseButtonStateListener
import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.shape.shape2d.Bounds
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.container.stack.template.layout.StackLike

import scala.concurrent.duration.Duration

@deprecated("Replaced with ContainerSelectionManager, which supports also non-stack containers", "v1.2")
object StackSelectionManager
{
	/**
	  * Creates a content manager for immutable items that don't represent state of any other object. No two different
	  * items will be linked in any way.
	  * @param stack Stack that will hold the displays
	  * @param selectionAreaDrawer A drawer that will highlight the selected area
	  * @param contentPointer Pointer to the displayed content
	  * @param equalsCheck Function for checking item equality (default = standard equals (== -operator))
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forStatelessItemsPointer[A, Display <: Stackable with Refreshable[A]]
	(stack: StackLike[Display] with CustomDrawable, selectionAreaDrawer: CustomDrawer, contentPointer: PointerWithEvents[Vector[A]],
	 equalsCheck: (A, A) => Boolean = { (a: A, b: A) => a == b })(makeDisplay: A => Display) =
		new StackSelectionManager[A, Display](stack, selectionAreaDrawer, contentPointer, equalsCheck)(makeDisplay)
	
	/**
	  * Creates a content manager for immutable items that don't represent state of any other object. No two different
	  * items will be linked in any way.
	  * @param stack Stack that will hold the displays
	  * @param selectionAreaDrawer A drawer that will highlight the selected area
	  * @param initialItems Initially displayed content (default = empty vector)
	  * @param equalsCheck Function for checking item equality (default = standard equals (== -operator))
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forStatelessItems[A, Display <: Stackable with Refreshable[A]]
	(stack: StackLike[Display] with CustomDrawable, selectionAreaDrawer: CustomDrawer, initialItems: Vector[A] = Vector(),
	 equalsCheck: (A, A) => Boolean = { (a: A, b: A) => a == b })(makeDisplay: A => Display) =
		forStatelessItemsPointer[A, Display](stack, selectionAreaDrawer, new PointerWithEvents(initialItems), equalsCheck)(makeDisplay)
	
	/**
	  * Creates a content manager for immutable items that represent a state of some other object
	  * (Eg. different immutable states of a single entity). The states may be linked together via a function
	  * (Eg. by checking related database item row id)
	  * @param stack Stack that will hold the displays
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
	def forImmutableStatesPointer[A, Display <: Stackable with Refreshable[A]]
	(stack: StackLike[Display] with CustomDrawable, selectionAreaDrawer: CustomDrawer, contentPointer: PointerWithEvents[Vector[A]])(
		sameItemCheck: (A, A) => Boolean)(makeDisplay: A => Display) =
		new StackSelectionManager[A, Display](stack, selectionAreaDrawer, contentPointer, sameItemCheck,
			Some((a: A, b: A) => a == b))(makeDisplay)
	
	/**
	  * Creates a content manager for immutable items that represent a state of some other object
	  * (Eg. different immutable states of a single entity). The states may be linked together via a function
	  * (Eg. by checking related database item row id)
	  * @param stack Stack that will hold the displays
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
	def forImmutableStates[A, Display <: Stackable with Refreshable[A]]
	(stack: StackLike[Display] with CustomDrawable, selectionAreaDrawer: CustomDrawer, initialItems: Vector[A] = Vector())(
		sameItemCheck: (A, A) => Boolean)(makeDisplay: A => Display) =
		forImmutableStatesPointer[A, Display](stack, selectionAreaDrawer, new PointerWithEvents(initialItems))(sameItemCheck)(makeDisplay)
	
	/**
	  * Creates a content manager for mutable / mutating items. Please note that the items may not always update
	  * correctly since mutations inside the content do not trigger content change events. Therefore you may manually
	  * need to trigger updates for the container's displays.
	  * @param stack Stack that will hold the displays
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
	def forMutableItemsPointer[A, Display <: Stackable with Refreshable[A]]
	(stack: StackLike[Display] with CustomDrawable, selectionAreaDrawer: CustomDrawer, contentPointer: PointerWithEvents[Vector[A]])(
		sameItemCheck: (A, A) => Boolean)(equalsCheck: (A, A) => Boolean)(makeDisplay: A => Display) =
		new StackSelectionManager[A, Display](stack, selectionAreaDrawer, contentPointer, sameItemCheck,
			Some(equalsCheck))(makeDisplay)
	
	/**
	  * Creates a content manager for mutable / mutating items. Please note that the items may not always update
	  * correctly since mutations inside the content do not trigger content change events. Therefore you may manually
	  * need to trigger updates for the container's displays.
	  * @param stack Stack that will hold the displays
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
	def forMutableItems[A, Display <: Stackable with Refreshable[A]]
	(stack: StackLike[Display] with CustomDrawable, selectionAreaDrawer: CustomDrawer, initialItems: Vector[A] = Vector())(
		sameItemCheck: (A, A) => Boolean)(equalsCheck: (A, A) => Boolean)(makeDisplay: A => Display) =
		forMutableItemsPointer[A, Display](stack, selectionAreaDrawer, new PointerWithEvents(initialItems))(
			sameItemCheck)(equalsCheck)(makeDisplay)
}

/**
  * This class handles content and selection in a stack
  * @author Mikko Hilpinen
  * @since 5.6.2019, v1+
  */
@deprecated("Replaced with ContainerSelectionManager, which supports also non-stack containers", "v1.2")
class StackSelectionManager[A, C <: Stackable with Refreshable[A]]
(stack: StackLike[C] with CustomDrawable, selectionAreaDrawer: CustomDrawer,
 contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
 sameItemCheck: EqualsFunction[A] = EqualsFunction.default, equalsCheck: Option[EqualsFunction[A]] = None)(makeItem: A => C)
	extends ContainerContentManager[A, StackLike[C], C](stack, contentPointer, sameItemCheck, equalsCheck)(makeItem) with SelectionManager[A, C]
{
	// INITIAL CODE	--------------------
	
	stack.addCustomDrawer(SelectionDrawer)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def updateSelectionDisplay(oldSelected: Option[C], newSelected: Option[C]) = stack.repaint()
	
	override protected def finalizeRefresh() =
	{
		super.finalizeRefresh()
		stack.repaint()
	}
	
	
	// OTHER	------------------------
	
	/**
	  * Enables mouse button state handling for the stack (selects the clicked item)
	  * @param consumeEvents Whether mouse events should be consumed (default = true)
	  */
	def enableMouseHandling(consumeEvents: Boolean = true) = stack.addMouseButtonListener(new MouseHandler(consumeEvents))
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
		stack.addKeyStateListener(listener)
		actorHandler += listener
	}
	
	
	// NESTED CLASSES	----------------
	
	private object SelectionDrawer extends CustomDrawer
	{
		override def opaque = false
		
		override def drawLevel = selectionAreaDrawer.drawLevel
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			// Draws the selected area using another custom drawer
			selectedDisplay.flatMap(stack.areaOf).foreach { area => selectionAreaDrawer.draw(drawer,
				area.translated(bounds.position)) }
		}
	}
	
	private class MouseHandler(val consumeEvents: Boolean) extends MouseButtonStateListener with Handleable
	{
		// Only considers left mouse button presses inside stack bounds
		override def mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter &&
			MouseEvent.isOverAreaFilter(stack.bounds)
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			val nearest = stack.itemNearestTo(event.mousePosition - stack.position)
			nearest.foreach(handleMouseClick)
			
			if (consumeEvents && nearest.isDefined)
				Some(ConsumeEvent("Stack selection change"))
			else
				None
		}
	}
}
