package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.EventHandler
import utopia.genesis.handling.event.keyboard.KeyDownEvent.KeyDownEventFilter
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object KeyDownHandler
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * A factory used for constructing new key-down event handlers
	 */
	val factory = KeyDownHandlerFactory()
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: KeyDownHandler.type): KeyDownHandlerFactory = factory
	
	
	// NESTED   --------------------------
	
	case class KeyDownHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[KeyDownListener, KeyDownHandler, KeyDownHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike): KeyDownHandlerFactory = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[KeyDownListener]): KeyDownHandler =
			new KeyDownHandler(initialItems, condition)
	}
}

/**
 * A handler used for distributing key-down events
 * @author Mikko Hilpinen
 * @since 29/03/2024, v4.0
 */
class KeyDownHandler(initialListeners: IterableOnce[KeyDownListener] = Iterable.empty,
                     additionalCondition: Changing[Boolean] = AlwaysTrue)
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
