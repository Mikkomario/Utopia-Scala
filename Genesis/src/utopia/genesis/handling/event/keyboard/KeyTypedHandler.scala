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

object KeyTypedHandler
{
	// COMPUTED   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	def factory(implicit log: Logger) = KeyTypedHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: KeyTypedHandler.type)(implicit log: Logger): KeyTypedHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class KeyTypedHandlerFactory(override val condition: Flag = AlwaysTrue)(implicit log: Logger)
		extends HandlerFactory[KeyTypedListener, KeyTypedHandler, KeyTypedHandlerFactory]
	{
		override def usingCondition(newCondition: Flag) = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[KeyTypedListener]) =
			new KeyTypedHandler(initialItems, condition)
	}
}

/**
  * A handler used for distributing key typed -events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v4.0
  */
class KeyTypedHandler(initialListeners: IterableOnce[KeyTypedListener] = Empty,
                      additionalCondition: Changing[Boolean] = AlwaysTrue)
                     (implicit log: Logger)
	extends DeepHandler[KeyTypedListener](initialListeners, additionalCondition)
		with EventHandler[KeyTypedListener, KeyTypedEvent] with KeyTypedListener
{
	override def keyTypedEventFilter: Filter[KeyTypedEvent] = AcceptAll
	
	override protected def filterOf(listener: KeyTypedListener): Filter[KeyTypedEvent] = listener.keyTypedEventFilter
	override protected def deliver(listener: KeyTypedListener, event: KeyTypedEvent): Unit =
		listener.onKeyTyped(event)
	
	override def onKeyTyped(event: KeyTypedEvent): Unit = distribute(event)
	
	override protected def asHandleable(item: Handleable): Option[KeyTypedListener] = item match {
		case l: KeyTypedListener => Some(l)
		case _ => None
	}
}
