package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.action.Actor
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import java.time.Instant
import scala.annotation.unused
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

object MouseOverHandler
{
	// COMPUTED   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	def factory(implicit log: Logger) = MouseOverHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseOverHandler.type)(implicit log: Logger): MouseOverHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseOverHandlerFactory(override val condition: Flag = AlwaysTrue)(implicit log: Logger)
		extends HandlerFactory[MouseOverListener, MouseOverHandler, MouseOverHandlerFactory]
	{
		override def usingCondition(newCondition: Flag) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseOverListener]) =
			new MouseOverHandler(initialItems, condition)
	}
}

/**
  * A handler used for generating mouse over -events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class MouseOverHandler(initialListeners: IterableOnce[MouseOverListener] = Empty,
                       additionalCondition: Changing[Boolean] = AlwaysTrue)
                      (implicit log: Logger)
	extends DeepHandler[MouseOverListener](initialListeners, additionalCondition) with Actor with MouseMoveListener
{
	// ATTRIBUTES   ----------------------
	
	// Items on which the mouse is currently hovering
	private var entries = Map[MouseOverListener, Instant]()
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
	
	override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
	
	override def onMouseMove(event: MouseMoveEvent): Unit = {
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
			val event = MouseOverEvent(lastPosition, CommonMouseEvents.buttonStates, duration, Now - enterTime)
			if (listener.mouseOverEventFilter(event))
				listener.onMouseOver(event)
		}
	}
	
	override protected def asHandleable(item: Handleable): Option[MouseOverListener] = item match {
		case l: MouseOverListener => Some(l)
		case _ => None
	}
}
