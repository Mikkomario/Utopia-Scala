package utopia.genesis.handling.event.animation

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.EventHandler2
import utopia.genesis.handling.event.animation.AnimationListener.AnimationEventFilter
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

import scala.annotation.unused
import scala.language.implicitConversions

object AnimationHandler
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A factory used for constructing these handlers
	  */
	val factory = AnimationHandlerFactory()
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: AnimationHandler.type): AnimationHandlerFactory = factory
	
	
	// NESTED   --------------------------
	
	case class AnimationHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[AnimationListener, AnimationHandler, AnimationHandlerFactory]
	{
		// IMPLEMENTED  ------------------
		
		override def usingCondition(newCondition: FlagLike): AnimationHandlerFactory = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[AnimationListener]): AnimationHandler =
			new AnimationHandler(initialItems, condition)
	}
}

/**
  * A handler used for distributing animation events
  * @author Mikko Hilpinen
  * @since 22/02/2024, v4.0
  */
class AnimationHandler(initialListeners: IterableOnce[AnimationListener] = Iterable.empty,
                       additionalCondition: FlagLike = AlwaysTrue)
	extends DeepHandler2[AnimationListener](initialListeners, additionalCondition)
		with EventHandler2[AnimationListener, AnimationEvent] with AnimationListener
{
	override def animationEventFilter: AnimationEventFilter = AcceptAll
	
	override def onAnimationEvent(event: AnimationEvent): Unit = distribute(event)
	
	override protected def filterOf(listener: AnimationListener): Filter[AnimationEvent] = listener.animationEventFilter
	override protected def deliver(listener: AnimationListener, event: AnimationEvent): Unit =
		listener.onAnimationEvent(event)
	
	override protected def asHandleable(item: Handleable2): Option[AnimationListener] = item match {
		case l: AnimationListener => Some(l)
		case _ => None
	}
}
