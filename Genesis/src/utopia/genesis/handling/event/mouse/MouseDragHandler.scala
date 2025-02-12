package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.EventHandler
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object MouseDragHandler
{
	// COMPUTED   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	def factory(implicit log: Logger) = MouseDragHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseDragHandler.type)(implicit log: Logger): MouseDragHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseDragHandlerFactory(override val condition: Flag = AlwaysTrue)(implicit log: Logger)
		extends HandlerFactory[MouseDragListener, MouseDragHandler, MouseDragHandlerFactory]
	{
		override def usingCondition(newCondition: Flag) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseDragListener]) =
			new MouseDragHandler(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse drag events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class MouseDragHandler(initialListeners: IterableOnce[MouseDragListener] = Empty,
                       additionalCondition: Changing[Boolean] = AlwaysTrue)
                      (implicit log: Logger)
	extends DeepHandler[MouseDragListener](initialListeners, additionalCondition)
		with EventHandler[MouseDragListener, MouseDragEvent] with MouseDragListener
{
	override def mouseDragEventFilter: Filter[MouseDragEvent] = AcceptAll
	
	override protected def filterOf(listener: MouseDragListener): Filter[MouseDragEvent] =
		listener.mouseDragEventFilter
	override protected def deliver(listener: MouseDragListener, event: MouseDragEvent): Unit =
		listener.onMouseDrag(event)
	
	override def onMouseDrag(event: MouseDragEvent): Unit = distribute(event)
	
	override protected def asHandleable(item: Handleable): Option[MouseDragListener] = item match {
		case l: MouseDragListener => Some(l)
		case _ => None
	}
}
