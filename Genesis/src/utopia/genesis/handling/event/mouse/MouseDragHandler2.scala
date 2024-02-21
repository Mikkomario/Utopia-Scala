package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.EventHandler2
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object MouseDragHandler2
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	val factory = MouseDragHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: MouseDragHandler2.type): MouseDragHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class MouseDragHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[MouseDragListener2, MouseDragHandler2, MouseDragHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[MouseDragListener2]) =
			new MouseDragHandler2(initialItems, condition)
	}
}

/**
  * A handler used for distributing mouse drag events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class MouseDragHandler2(initialListeners: IterableOnce[MouseDragListener2] = Iterable.empty,
                        additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler2[MouseDragListener2](initialListeners, additionalCondition)
		with EventHandler2[MouseDragListener2, MouseDragEvent2] with MouseDragListener2
{
	override def mouseDragEventFilter: Filter[MouseDragEvent2] = AcceptAll
	
	override protected def filterOf(listener: MouseDragListener2): Filter[MouseDragEvent2] =
		listener.mouseDragEventFilter
	override protected def deliver(listener: MouseDragListener2, event: MouseDragEvent2): Unit =
		listener.onMouseDrag(event)
	
	override def onMouseDrag(event: MouseDragEvent2): Unit = distribute(event)
	
	override protected def asHandleable(item: Handleable2): Option[MouseDragListener2] = item match {
		case l: MouseDragListener2 => Some(l)
		case _ => None
	}
}
