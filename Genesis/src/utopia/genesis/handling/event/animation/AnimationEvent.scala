package utopia.genesis.handling.event.animation

import utopia.flow.operator.filter.Filter

/**
  * Common trait for events fired during animation-processing
  * @author Mikko Hilpinen
  * @since 22/02/2024, v4.0
  */
sealed trait AnimationEvent
{
	// ABSTRACT --------------------
	
	/**
	  * @return Current animation progress. Between 0 (start) and 1 (end).
	  */
	def progress: Double
	
	/**
	  * @return Whether the animation will continue after this event
	  */
	def continues: Boolean
	
	
	// COMPUTED -------------------
	
	/**
	  * @return Whether the animation will not progress forward after this event
	  */
	def stops = !continues
}

object AnimationEvent
{
	// TYPES    ------------------
	
	/**
	  * Filter applied to animation events
	  */
	type AnimationEventFilter = Filter[AnimationEvent]
	
	
	// COMPUTED  -----------------
	
	/**
	  * @return Access to filters that may be applied to these events
	  */
	def filter = AnimationEventFilter
	
	
	// VALUES   ------------------
	
	/**
	  * An event fired when an animation first starts running, continues after being stopped,
	  * or is restarted from the beginning without completing first.
	  */
	case class Started(from: Double = 0.0) extends AnimationEvent
	{
		override def progress: Double = from
		override def continues: Boolean = true
		
		override def toString = s"Animation started from $from"
	}
	
	/**
	  * An event fired when an animation is paused or stopped at some state other than its completion.
	  * Also fired if the animation is moved to a different state without continuing it.
	  * @param progress Animation progress [0,1] where the animation was paused
	  */
	case class Paused(progress: Double) extends AnimationEvent
	{
		override def continues: Boolean = false
		
		override def toString = "Animation paused"
	}
	
	/**
	  * An event fired when an animation completes
	  * @param loops Whether the animation will start again afterwards.
	  */
	case class Completed(loops: Boolean) extends AnimationEvent
	{
		// IMPLEMENTED  -------------------
		
		override def progress: Double = 1.0
		override def continues: Boolean = loops
		
		override def toString = if (loops) "Animation loops" else "Animation completed"
	}
	
	
	// NESTED   ---------------------------
	
	trait AnimationFilteringFactory[+A] extends Any
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
	
	
	// EXTENSIONS   --------------------------
	
	implicit class RichAnimationEventFilter(val f: AnimationEventFilter)
		extends AnyVal with AnimationFilteringFactory[AnimationEventFilter]
	{
		override protected def withFilter(filter: AnimationEventFilter): AnimationEventFilter = f && filter
	}
}
