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

object MouseMoveHandler
{
	// COMPUTED   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	def factory(implicit log: Logger) = MouseMoveHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseMoveHandler.type)(implicit log: Logger): MouseMoveHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseMoveHandlerFactory(override val condition: Flag = AlwaysTrue)(implicit log: Logger)
		extends HandlerFactory[MouseMoveListener, MouseMoveHandler, MouseMoveHandlerFactory]
	{
		override def usingCondition(newCondition: Flag) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseMoveListener]) =
			new MouseMoveHandler(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse moved -events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class MouseMoveHandler(initialListeners: IterableOnce[MouseMoveListener] = Empty,
                       additionalCondition: Changing[Boolean] = AlwaysTrue)
                      (implicit log: Logger)
	extends DeepHandler[MouseMoveListener](initialListeners, additionalCondition)
		with EventHandler[MouseMoveListener, MouseMoveEvent] with MouseMoveListener
{
	override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
	
	override protected def filterOf(listener: MouseMoveListener): Filter[MouseMoveEvent] =
		listener.mouseMoveEventFilter
	override protected def deliver(listener: MouseMoveListener, event: MouseMoveEvent): Unit =
		listener.onMouseMove(event)
	
	override protected def asHandleable(item: Handleable): Option[MouseMoveListener] = item match {
		case l: MouseMoveListener => Some(l)
		case _ => None
	}
	
	override def onMouseMove(event: MouseMoveEvent): Unit = distribute(event)
}
