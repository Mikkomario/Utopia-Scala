package utopia.reflection.controller.data

import java.awt.event.KeyEvent

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.TimeExtensions._
import utopia.genesis.event.{ConsumeEvent, MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.MouseButtonStateListener
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.Refreshable
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.stack.Stackable
import utopia.reflection.container.stack.StackLike

import scala.concurrent.duration.Duration

/**
  * This class handles content and selection in a stack
  * @author Mikko Hilpinen
  * @since 5.6.2019, v1+
  */
class StackSelectionManager[A, C <: Stackable with Refreshable[A]]
(stack: StackLike[C] with CustomDrawable, selectionAreaDrawer: CustomDrawer,
 equalsCheck: (A, A) => Boolean = { (a: A, b: A) => a == b },
 contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()))(makeItem: A => C)
	extends ContainerContentManager[A, StackLike[C], C](stack, equalsCheck, contentPointer)(makeItem) with SelectionManager[A, C]
{
	// INITIAL CODE	--------------------
	
	stack.addCustomDrawer(new SelectionDrawer)
	
	
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
	
	private class SelectionDrawer extends CustomDrawer
	{
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
