package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.consume.{ConsumableEventHandler, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object MouseButtonStateHandler
{
	// COMPUTED   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	def factory(implicit log: Logger) = MouseButtonStateHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseButtonStateHandler.type)(implicit log: Logger): MouseButtonStateHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseButtonStateHandlerFactory(override val condition: Flag = AlwaysTrue)(implicit log: Logger)
		extends HandlerFactory[MouseButtonStateListener, MouseButtonStateHandler, MouseButtonStateHandlerFactory]
	{
		override def usingCondition(newCondition: Flag) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseButtonStateListener]) =
			new MouseButtonStateHandler(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse button state events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v4.0
  */
class MouseButtonStateHandler(initialListeners: IterableOnce[MouseButtonStateListener] = Empty,
                              additionalCondition: Changing[Boolean] = AlwaysTrue)
                             (implicit log: Logger)
	extends DeepHandler[MouseButtonStateListener](initialListeners, additionalCondition)
		with ConsumableEventHandler[MouseButtonStateListener, MouseButtonStateEvent] with MouseButtonStateListener
{
	// IMPLEMENTED  ---------------------
	
	override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] = AcceptAll
	
	override protected def filterOf(listener: MouseButtonStateListener): Filter[MouseButtonStateEvent] =
		listener.mouseButtonStateEventFilter
	override protected def deliver(listener: MouseButtonStateListener, event: MouseButtonStateEvent): ConsumeChoice =
		listener.onMouseButtonStateEvent(event)
	
	override protected def asHandleable(item: Handleable): Option[MouseButtonStateListener] = item match {
		case l: MouseButtonStateListener => Some(l)
		case _ => None
	}
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = distribute(event)._2
}
