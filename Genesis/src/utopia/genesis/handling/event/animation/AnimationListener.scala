package utopia.genesis.handling.event.animation

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.animation.AnimationEvent.{AnimationEventFilter, AnimationFilteringFactory}
import utopia.genesis.handling.template.Handleable

import scala.annotation.unused
import scala.language.implicitConversions

object AnimationListener
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A factory used for constructing animation listeners without applying any conditions
	  */
	val unconditional = AnimationListenerFactory()
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: AnimationListener.type): AnimationListenerFactory = unconditional
	
	
	// NESTED   --------------------------
	
	case class AnimationListenerFactory(condition: FlagLike = AlwaysTrue, filter: AnimationEventFilter = AcceptAll)
		extends ListenerFactory[AnimationEvent, AnimationListenerFactory]
			with AnimationFilteringFactory[AnimationListenerFactory]
	{
		// IMPLEMENTED  ---------------------
		
		override def usingFilter(filter: Filter[AnimationEvent]): AnimationListenerFactory = copy(filter = filter)
		override def usingCondition(condition: FlagLike): AnimationListenerFactory = copy(condition = condition)
		
		override protected def withFilter(filter: AnimationEventFilter): AnimationListenerFactory =
			copy(filter = this.filter && filter)
			
		
		// OTHER    ------------------------
		
		/**
		  * @param f A function to call on accepted animation events
		  * @tparam U Arbitrary function result type
		  * @return A listener that calls the specified function
		  */
		def apply[U](f: AnimationEvent => U): AnimationListener = new _AnimationListener[U](condition, filter, f)
	}
	
	private class _AnimationListener[U](override val handleCondition: FlagLike,
	                                    override val animationEventFilter: AnimationEventFilter, f: AnimationEvent => U)
		extends AnimationListener
	{
		override def onAnimationEvent(event: AnimationEvent): Unit = f(event)
	}
}

/**
  * Common trait for items which are interested in receiving animation-related events
  * @author Mikko Hilpinen
  * @since 22/02/2024, v4.0
  */
trait AnimationListener extends Handleable
{
	/**
	  * @return Filter applied to incoming animation events.
	  *         Only events accepted by this filter should be delivered to .[[onAnimationEvent]](AnimationEvent)
	  */
	def animationEventFilter: AnimationEventFilter
	
	/**
	  * Called when a listened animation fires an event
	  * @param event A recent animation event
	  */
	def onAnimationEvent(event: AnimationEvent): Unit
}
