package utopia.genesis.handling.event.keyboard

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.EventHandler
import utopia.genesis.handling.event.keyboard.KeyDownEvent.KeyDownEventFilter
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object KeyDownHandler
{
	// COMPUTED   ----------------------
	
	/**
	 * A factory used for constructing new key-down event handlers
	 */
	def factory(implicit log: Logger) = KeyDownHandlerFactory()
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: KeyDownHandler.type)(implicit log: Logger): KeyDownHandlerFactory = factory
	
	
	// NESTED   --------------------------
	
	case class KeyDownHandlerFactory(override val condition: Flag = AlwaysTrue)(implicit log: Logger)
		extends HandlerFactory[KeyDownListener, KeyDownHandler, KeyDownHandlerFactory]
	{
		override def usingCondition(newCondition: Flag): KeyDownHandlerFactory = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[KeyDownListener]): KeyDownHandler =
			new KeyDownHandler(initialItems, condition)
	}
}

/**
 * A handler used for distributing key-down events
 * @author Mikko Hilpinen
 * @since 29/03/2024, v4.0
 */
class KeyDownHandler(initialListeners: IterableOnce[KeyDownListener] = Empty,
                     additionalCondition: Changing[Boolean] = AlwaysTrue)
                    (implicit log: Logger)
	extends DeepHandler[KeyDownListener](initialListeners, additionalCondition)
		with EventHandler[KeyDownListener, KeyDownEvent] with KeyDownListener
{
	override def keyDownEventFilter: KeyDownEventFilter = AcceptAll
	
	override def whileKeyDown(event: KeyDownEvent): Unit = distribute(event)
	
	override protected def filterOf(listener: KeyDownListener): Filter[KeyDownEvent] = listener.keyDownEventFilter
	override protected def deliver(listener: KeyDownListener, event: KeyDownEvent): Unit = listener.whileKeyDown(event)
	
	override protected def asHandleable(item: Handleable): Option[KeyDownListener] = item match {
		case l: KeyDownListener => Some(l)
		case _ => None
	}
}
