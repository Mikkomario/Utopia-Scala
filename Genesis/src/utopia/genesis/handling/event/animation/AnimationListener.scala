package utopia.genesis.handling.event.animation

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.animation.AnimationEvent.{Completed, Paused, Started}
import utopia.genesis.handling.event.animation.AnimationListener.AnimationEventFilter
import utopia.genesis.handling.template.Handleable2

import scala.annotation.unused
import scala.language.implicitConversions

object AnimationListener
{
	// TYPES    --------------------------
	
	/**
	  * Filter applied to animation events
	  */
	type AnimationEventFilter = Filter[AnimationEvent]
	
	
	// ATTRIBUTES   ----------------------
	
	/**
	  * A factory used for constructing animation listeners without applying any conditions
	  */
	val unconditional = AnimationListenerFactory()
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Access to animation event filters
	  */
	def filter = AnimationEventFilter
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: AnimationListener.type): AnimationListenerFactory = unconditional
	
	
	// NESTED   --------------------------
	
	trait AnimationFilteringFactory[+A]
	{
		// ABSTRACT ----------------------
		
		/**
		  * @param filter A filter to apply
		  * @return An item with that filter applied to it
		  */
		protected def withFilter(filter: AnimationEventFilter): A
		
		
		// COMPUTED -----------------------
		
		/**
		  * @return An item that only accepts animation started or resumed -events
		  */
		def start = withFilter { _.isInstanceOf[Started] }
		/**
		  * @return An item that only accepts animation paused -events
		  */
		def pause = withFilter { _.isInstanceOf[Paused] }
		/**
		  * @return An item that only accepts animation completion events where the animation
		  *         will continue to loop
		  */
		def loop = withFilter {
			case Completed(loops) => loops
			case _ => false
		}
		/**
		  * @return An item that only accepts animation completion events where the animation will end / stop
		  */
		def finish = withFilter {
			case Completed(loops) => !loops
			case _ => false
		}
		
		/**
		  * @return An item that accepts events where animation will continue afterwards
		  */
		def continues = withFilter { _.continues }
		/**
		  * @return An item that accepts events where the animation will not progress afterwards
		  */
		def stops = withFilter { _.stops }
		
		/**
		  * @return An item that accepts events where the animation is at the beginning
		  */
		def beginning = withFilter { _.progress <= 0.0 }
		/**
		  * @return An item that accepts events where the animation is at the end
		  */
		def end = withFilter { _.progress >= 1.0 }
		/**
		  * @return An item that accepts events where the animation is somewhere between its start and its end
		  */
		def middle = withFilter { e => e.progress > 0.0 && e.progress < 1.0 }
	}
	
	case object AnimationEventFilter extends AnimationFilteringFactory[AnimationEventFilter]
	{
		// IMPLEMENTED  --------------------
		
		override protected def withFilter(filter: AnimationEventFilter): AnimationEventFilter = filter
		
		
		// OTHER    -----------------------
		
		/**
		  * @param f A filtering function
		  * @return A filter that utilizes that function
		  */
		def apply(f: AnimationEvent => Boolean): AnimationEventFilter = Filter(f)
	}
	
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
trait AnimationListener extends Handleable2
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
