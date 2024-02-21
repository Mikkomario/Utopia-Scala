package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.consume.{ConsumableEventHandler2, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object MouseButtonStateHandler
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	val factory = MouseButtonStateHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseButtonStateHandler.type): MouseButtonStateHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseButtonStateHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[MouseButtonStateListener, MouseButtonStateHandler, MouseButtonStateHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseButtonStateListener]) =
			new MouseButtonStateHandler(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse button state events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v4.0
  */
class MouseButtonStateHandler(initialListeners: IterableOnce[MouseButtonStateListener] = Iterable.empty,
                              additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler2[MouseButtonStateListener](initialListeners, additionalCondition)
		with ConsumableEventHandler2[MouseButtonStateListener, MouseButtonStateEvent] with MouseButtonStateListener
{
	// IMPLEMENTED  ---------------------
	
	override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] = AcceptAll
	
	override protected def filterOf(listener: MouseButtonStateListener): Filter[MouseButtonStateEvent] =
		listener.mouseButtonStateEventFilter
	override protected def deliver(listener: MouseButtonStateListener, event: MouseButtonStateEvent): ConsumeChoice =
		listener.onMouseButtonStateEvent(event)
	
	override protected def asHandleable(item: Handleable2): Option[MouseButtonStateListener] = item match {
		case l: MouseButtonStateListener => Some(l)
		case _ => None
	}
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = distribute(event)._2
}
