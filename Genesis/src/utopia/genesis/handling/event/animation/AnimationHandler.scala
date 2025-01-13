package utopia.genesis.handling.event.animation

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.EventHandler
import utopia.genesis.handling.event.animation.AnimationEvent.AnimationEventFilter
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object AnimationHandler
{
	// COMPUTED   ----------------------
	
	/**
	  * A factory used for constructing these handlers
	  */
	def factory(implicit log: Logger) = AnimationHandlerFactory()
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: AnimationHandler.type)(implicit log: Logger): AnimationHandlerFactory =
		factory
	
	
	// NESTED   --------------------------
	
	case class AnimationHandlerFactory(override val condition: Flag = AlwaysTrue)(implicit log: Logger)
		extends HandlerFactory[AnimationListener, AnimationHandler, AnimationHandlerFactory]
	{
		// IMPLEMENTED  ------------------
		
		override def usingCondition(newCondition: Flag): AnimationHandlerFactory = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[AnimationListener]): AnimationHandler =
			new AnimationHandler(initialItems, condition)
	}
}

/**
  * A handler used for distributing animation events
  * @author Mikko Hilpinen
  * @since 22/02/2024, v4.0
  */
class AnimationHandler(initialListeners: IterableOnce[AnimationListener] = Empty,
                       additionalCondition: Flag = AlwaysTrue)
                      (implicit log: Logger)
	extends DeepHandler[AnimationListener](initialListeners, additionalCondition)
		with EventHandler[AnimationListener, AnimationEvent] with AnimationListener
{
	override def animationEventFilter: AnimationEventFilter = AcceptAll
	
	override def onAnimationEvent(event: AnimationEvent): Unit = distribute(event)
	
	override protected def filterOf(listener: AnimationListener): Filter[AnimationEvent] = listener.animationEventFilter
	override protected def deliver(listener: AnimationListener, event: AnimationEvent): Unit =
		listener.onAnimationEvent(event)
	
	override protected def asHandleable(item: Handleable): Option[AnimationListener] = item match {
		case l: AnimationListener => Some(l)
		case _ => None
	}
}
