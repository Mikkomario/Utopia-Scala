package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.consume.{ConsumableEventHandler, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object MouseWheelHandler
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	val factory = MouseWheelHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseWheelHandler.type): MouseWheelHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseWheelHandlerFactory(override val condition: Flag = AlwaysTrue)
		extends HandlerFactory[MouseWheelListener, MouseWheelHandler, MouseWheelHandlerFactory]
	{
		override def usingCondition(newCondition: Flag) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseWheelListener]) =
			new MouseWheelHandler(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse wheel events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class MouseWheelHandler(initialListeners: IterableOnce[MouseWheelListener] = Empty,
                        additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler[MouseWheelListener](initialListeners, additionalCondition)
		with ConsumableEventHandler[MouseWheelListener, MouseWheelEvent] with MouseWheelListener
{
	override def mouseWheelEventFilter: Filter[MouseWheelEvent] = AcceptAll
	
	override protected def filterOf(listener: MouseWheelListener): Filter[MouseWheelEvent] =
		listener.mouseWheelEventFilter
	override protected def deliver(listener: MouseWheelListener, event: MouseWheelEvent): ConsumeChoice =
		listener.onMouseWheelRotated(event)
	
	override def onMouseWheelRotated(event: MouseWheelEvent): ConsumeChoice = distribute(event)._2
	
	override protected def asHandleable(item: Handleable): Option[MouseWheelListener] = item match {
		case l: MouseWheelListener => Some(l)
		case _ => None
	}
}
