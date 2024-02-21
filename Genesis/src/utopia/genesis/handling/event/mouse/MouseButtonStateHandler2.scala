package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.consume.{ConsumableEventHandler2, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object MouseButtonStateHandler2
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	val factory = MouseButtonStateHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseButtonStateHandler2.type): MouseButtonStateHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseButtonStateHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[MouseButtonStateListener2, MouseButtonStateHandler2, MouseButtonStateHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseButtonStateListener2]) =
			new MouseButtonStateHandler2(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse button state events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v4.0
  */
class MouseButtonStateHandler2(initialListeners: IterableOnce[MouseButtonStateListener2] = Iterable.empty,
                               additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler2[MouseButtonStateListener2](initialListeners, additionalCondition)
		with ConsumableEventHandler2[MouseButtonStateListener2, MouseButtonStateEvent2] with MouseButtonStateListener2
{
	// IMPLEMENTED  ---------------------
	
	override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent2] = AcceptAll
	
	override protected def filterOf(listener: MouseButtonStateListener2): Filter[MouseButtonStateEvent2] =
		listener.mouseButtonStateEventFilter
	override protected def deliver(listener: MouseButtonStateListener2, event: MouseButtonStateEvent2): ConsumeChoice =
		listener.onMouseButtonStateEvent(event)
	
	override protected def asHandleable(item: Handleable2): Option[MouseButtonStateListener2] = item match {
		case l: MouseButtonStateListener2 => Some(l)
		case _ => None
	}
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent2): ConsumeChoice = distribute(event)._2
}
