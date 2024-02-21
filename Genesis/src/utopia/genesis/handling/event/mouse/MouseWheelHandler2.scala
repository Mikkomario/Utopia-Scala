package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.consume.{ConsumableEventHandler2, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object MouseWheelHandler2
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	val factory = MouseWheelHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseWheelHandler2.type): MouseWheelHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseWheelHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[MouseWheelListener2, MouseWheelHandler2, MouseWheelHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseWheelListener2]) =
			new MouseWheelHandler2(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse wheel events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class MouseWheelHandler2(initialListeners: IterableOnce[MouseWheelListener2] = Iterable.empty,
                         additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler2[MouseWheelListener2](initialListeners, additionalCondition)
		with ConsumableEventHandler2[MouseWheelListener2, MouseWheelEvent2] with MouseWheelListener2
{
	override def mouseWheelEventFilter: Filter[MouseWheelEvent2] = AcceptAll
	
	override protected def filterOf(listener: MouseWheelListener2): Filter[MouseWheelEvent2] =
		listener.mouseWheelEventFilter
	override protected def deliver(listener: MouseWheelListener2, event: MouseWheelEvent2): ConsumeChoice =
		listener.onMouseWheelRotated(event)
	
	override def onMouseWheelRotated(event: MouseWheelEvent2): ConsumeChoice = distribute(event)._2
	
	override protected def asHandleable(item: Handleable2): Option[MouseWheelListener2] = item match {
		case l: MouseWheelListener2 => Some(l)
		case _ => None
	}
}
