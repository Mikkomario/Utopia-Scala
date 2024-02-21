package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.EventHandler2
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

import scala.annotation.unused

object KeyTypedHandler2
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	val factory = KeyTypedHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: KeyTypedHandler2.type): KeyTypedHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class KeyTypedHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[KeyTypedListener2, KeyTypedHandler2, KeyTypedHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[KeyTypedListener2]) =
			new KeyTypedHandler2(initialItems, condition)
	}
}

/**
  * A handler used for distributing key typed -events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v4.0
  */
class KeyTypedHandler2(initialListeners: IterableOnce[KeyTypedListener2] = Iterable.empty,
                       additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler2[KeyTypedListener2](initialListeners, additionalCondition)
		with EventHandler2[KeyTypedListener2, KeyTypedEvent2] with KeyTypedListener2
{
	override def keyTypedEventFilter: Filter[KeyTypedEvent2] = AcceptAll
	
	override protected def filterOf(listener: KeyTypedListener2): Filter[KeyTypedEvent2] = listener.keyTypedEventFilter
	override protected def deliver(listener: KeyTypedListener2, event: KeyTypedEvent2): Unit =
		listener.onKeyTyped(event)
	
	override def onKeyTyped(event: KeyTypedEvent2): Unit = distribute(event)
	
	override protected def asHandleable(item: Handleable2): Option[KeyTypedListener2] = item match {
		case l: KeyTypedListener2 => Some(l)
		case _ => None
	}
}
