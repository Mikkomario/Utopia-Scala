package utopia.genesis.handling.event.keyboard

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.EventHandler
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object KeyStateHandler
{
	// COMPUTED   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	def factory(implicit log: Logger) = KeyStateHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: KeyStateHandler.type)(implicit log: Logger): KeyStateHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class KeyStateHandlerFactory(override val condition: Flag = AlwaysTrue)(implicit log: Logger)
		extends HandlerFactory[KeyStateListener, KeyStateHandler, KeyStateHandlerFactory]
	{
		override def usingCondition(newCondition: Flag) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[KeyStateListener]) =
			new KeyStateHandler(initialItems, condition)
	}
}

/**
  * A handler that distributes key-state events
  * @author Mikko Hilpinen
  * @since 03/02/2024, v4.0
  */
class KeyStateHandler(initialListeners: IterableOnce[KeyStateListener] = Empty,
                      additionalCondition: Changing[Boolean] = AlwaysTrue)
                     (implicit log: Logger)
	extends DeepHandler[KeyStateListener](initialListeners, additionalCondition)
		with EventHandler[KeyStateListener, KeyStateEvent] with KeyStateListener
{
	override def keyStateEventFilter: Filter[KeyStateEvent] = AcceptAll
	
	override def onKeyState(event: KeyStateEvent): Unit = distribute(event)
	
	override protected def filterOf(listener: KeyStateListener) =
		listener.keyStateEventFilter
	override protected def deliver(listener: KeyStateListener, event: KeyStateEvent): Unit =
		listener.onKeyState(event)
	
	override protected def asHandleable(item: Handleable): Option[KeyStateListener] = item match {
		case l: KeyStateListener => Some(l)
		case _ => None
	}
}
