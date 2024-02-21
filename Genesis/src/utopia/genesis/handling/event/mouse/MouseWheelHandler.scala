package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.consume.{ConsumableEventHandler2, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

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
	
	case class MouseWheelHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[MouseWheelListener, MouseWheelHandler, MouseWheelHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseWheelListener]) =
			new MouseWheelHandler(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse wheel events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class MouseWheelHandler(initialListeners: IterableOnce[MouseWheelListener] = Iterable.empty,
                        additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler2[MouseWheelListener](initialListeners, additionalCondition)
		with ConsumableEventHandler2[MouseWheelListener, MouseWheelEvent] with MouseWheelListener
{
	override def mouseWheelEventFilter: Filter[MouseWheelEvent] = AcceptAll
	
	override protected def filterOf(listener: MouseWheelListener): Filter[MouseWheelEvent] =
		listener.mouseWheelEventFilter
	override protected def deliver(listener: MouseWheelListener, event: MouseWheelEvent): ConsumeChoice =
		listener.onMouseWheelRotated(event)
	
	override def onMouseWheelRotated(event: MouseWheelEvent): ConsumeChoice = distribute(event)._2
	
	override protected def asHandleable(item: Handleable2): Option[MouseWheelListener] = item match {
		case l: MouseWheelListener => Some(l)
		case _ => None
	}
}
