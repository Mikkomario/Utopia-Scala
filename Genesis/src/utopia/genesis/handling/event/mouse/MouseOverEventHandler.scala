package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.genesis.handling.action.Actor2
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

/**
  * A handler used for generating mouse over -events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v3.6
  */
class MouseOverEventHandler(initialListeners: IterableOnce[MouseOverListener2] = Vector.empty)
	extends DeepHandler2[MouseOverListener2](initialListeners) with Actor2 with MouseMoveListener2
{
	// ATTRIBUTES   ----------------------
	
	// Items on which the mouse is currently hovering
	private var entries = Map[MouseOverListener2, Instant]()
	private var lastPosition = RelativePoint.origin
	
	
	// INITIAL CODE ----------------------
	
	itemsPointer.addContinuousListener { e =>
		// Checks which items were removed and which added
		val (changes, _) = e.values.separateMatching
		lazy val now = Now.toInstant
		// Updates the list of mouse-over-items accordingly
		entries = entries.filterNot { case (l, _) => changes.first.contains(l) } ++
			changes.second.filter { _.contains(lastPosition) }.map { _ -> now }
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override def mouseMoveEventFilter: Filter[MouseMoveEvent2] = AcceptAll
	
	override def onMouseMove(event: MouseMoveEvent2): Unit = {
		// Updates the mouse position
		lastPosition = event.position
		
		// Checks for exits and entries
		val remainingEntries = entries.filter { _._1.contains(event.position) }
		val newEntries = items.filter { a => !entries.contains(a) && a.contains(event.position) }
		
		// Updates the list of mouse-over-items
		if (newEntries.nonEmpty) {
			val now = Now.toInstant
			entries = remainingEntries ++ newEntries.map { _ -> now }
		}
		else
			entries = remainingEntries
	}
	
	override def act(duration: FiniteDuration): Unit = {
		// Informs each mouse-over-item that the mouse is still hovering over them
		entries.foreach { case (listener, enterTime) =>
			// FIXME: Apply the common mouse button states here
			val event = MouseOverEvent(lastPosition, MouseButtonStates.default, duration, Now - enterTime)
			if (listener.mouseOverEventFilter(event))
				listener.onMouseOver(event)
		}
	}
	
	override protected def asHandleable(item: Handleable2): Option[MouseOverListener2] = item match {
		case l: MouseOverListener2 => Some(l)
		case _ => None
	}
}
