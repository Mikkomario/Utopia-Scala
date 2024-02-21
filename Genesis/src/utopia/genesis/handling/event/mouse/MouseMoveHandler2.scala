package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.EventHandler2
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object MouseMoveHandler2
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	val factory = MouseMoveHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseMoveHandler2.type): MouseMoveHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseMoveHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[MouseMoveListener2, MouseMoveHandler2, MouseMoveHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseMoveListener2]) =
			new MouseMoveHandler2(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse moved -events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class MouseMoveHandler2(initialListeners: IterableOnce[MouseMoveListener2] = Iterable.empty,
                        additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler2[MouseMoveListener2](initialListeners, additionalCondition)
		with EventHandler2[MouseMoveListener2, MouseMoveEvent2] with MouseMoveListener2
{
	override def mouseMoveEventFilter: Filter[MouseMoveEvent2] = AcceptAll
	
	override protected def filterOf(listener: MouseMoveListener2): Filter[MouseMoveEvent2] =
		listener.mouseMoveEventFilter
	override protected def deliver(listener: MouseMoveListener2, event: MouseMoveEvent2): Unit =
		listener.onMouseMove(event)
	
	override protected def asHandleable(item: Handleable2): Option[MouseMoveListener2] = item match {
		case l: MouseMoveListener2 => Some(l)
		case _ => None
	}
	
	override def onMouseMove(event: MouseMoveEvent2): Unit = distribute(event)
}
