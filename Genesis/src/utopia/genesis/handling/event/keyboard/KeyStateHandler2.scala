package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.EventHandler2
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object KeyStateHandler2
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	val factory = KeyStateHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: KeyStateHandler2.type): KeyStateHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class KeyStateHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[KeyStateListener2, KeyStateHandler2, KeyStateHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[KeyStateListener2]) =
			new KeyStateHandler2(initialItems, condition)
	}
}

/**
  * A handler that distributes key-state events
  * @author Mikko Hilpinen
  * @since 03/02/2024, v4.0
  */
class KeyStateHandler2(initialListeners: IterableOnce[KeyStateListener2] = Iterable.empty,
                       additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler2[KeyStateListener2](initialListeners, additionalCondition)
		with EventHandler2[KeyStateListener2, KeyStateEvent2] with KeyStateListener2
{
	override def keyStateEventFilter: Filter[KeyStateEvent2] = AcceptAll
	
	override def onKeyState(event: KeyStateEvent2): Unit = distribute(event)
	
	override protected def filterOf(listener: KeyStateListener2) =
		listener.keyStateEventFilter
	override protected def deliver(listener: KeyStateListener2, event: KeyStateEvent2): Unit =
		listener.onKeyState(event)
	
	override protected def asHandleable(item: Handleable2): Option[KeyStateListener2] = item match {
		case l: KeyStateListener2 => Some(l)
		case _ => None
	}
}
