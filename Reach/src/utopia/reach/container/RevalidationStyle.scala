package utopia.reach.container

import utopia.flow.collection.immutable.range.Span
import utopia.flow.operator.combine.{Combinable, LinearScalable}
import utopia.flow.time.TimeExtensions._

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * An enumeration for different component hierarchy revalidation implementation approaches
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
sealed trait RevalidationStyle
{
	// ABSTRACT ---------------------
	
	/**
	  * @return Whether the revalidation shall be performed immediately upon the call on
	  *         revalidate()
	  */
	def isImmediate: Boolean
	/**
	  * @return Whether the revalidation process shall be performed asynchronously
	  */
	def isAsynchronous: Boolean
	/**
	  * @return The minimum and the maximum delay before the revalidation shall be performed after calling revalidate()
	  */
	def delay: Span[FiniteDuration]
	
	
	// COMPUTED ---------------------
	
	/**
	  * @return Whether the revalidation shall not be performed immediately upon the call on revalidate()
	  */
	def isDelayed = !isImmediate
	/**
	  * @return Whether the revalidation process shall block the thread that called revalidate()
	  */
	def isBlocking = !isAsynchronous
	
	/**
	  * @return The minimum delay between the call on revalidate() and the revalidation process
	  */
	def minDelay = delay.start
	/**
	  * @return The maximum delay between the call on revalidate() and the revalidation process
	  */
	def maxDelay = delay.end
}

object RevalidationStyle
{
	object Immediate
	{
		/**
		  * A revalidation style where the revalidation process is initiated as quickly as possible,
		  * yet run in a separate thread
		  */
		val async = apply()
		/**
		  * A revalidation style where the revalidation process is run then and there, when revalidate() is called.
		  * One shall be careful for deadlocks when taking this approach.
		  */
		val blocking = apply(blocks = true)
	}
	/**
	  * A revalidation style that performs the revalidation immediately once revalidate() is called
	  * @param blocks Whether the implementation shall block the thread calling revalidate()
	  */
	case class Immediate(blocks: Boolean = false) extends RevalidationStyle
	{
		// COMPUTED ----------------------------
		
		/**
		  * @return A copy of this style which blocks
		  */
		def blocking = if (blocks) this else copy(blocks = true)
		/**
		  * @return A copy of this style which is asynchronous
		  */
		def async = if (blocks) copy(blocks = false) else this
		
		
		// IMPLEMENTED  ------------------------
		
		override def isImmediate: Boolean = true
		override def isAsynchronous: Boolean = !blocks
		override def delay: Span[FiniteDuration] = Span(Duration.Zero, Duration.Zero)
		
		override def minDelay = Duration.Zero
		override def maxDelay = Duration.Zero
	}
	
	object Delayed
	{
		/**
		  * @param amount The amount of delay between the call on revalidate() and the actual
		  *               initiation of the revalidation process
		  * @return A new revalidation style where revalidation is delayed by the specified amount
		  */
		def by(amount: FiniteDuration) = apply(Span(amount, amount))
		/**
		  * @param atLeast Minimum delay
		  * @param atMost Maximum delay
		  * @return A revalidation style where the revalidation is performed between 'atLeast' and 'atMost' after
		  *         the call on revalidate(). Revalidation is delayed when revalidate() is called repeatedly.
		  */
		def by(atLeast: FiniteDuration, atMost: FiniteDuration) = apply(Span(atLeast, atMost))
	}
	/**
	  * A revalidation approach where the revalidation implementation is delayed
	  * @param by How much the revalidation shall be delayed (minimum & maximum)
	  */
	case class Delayed(by: Span[FiniteDuration])
		extends RevalidationStyle with LinearScalable[Delayed] with Combinable[FiniteDuration, Delayed]
	{
		// IMPLEMENTED  -------------------------
		
		override def isImmediate: Boolean = minDelay <= Duration.Zero
		override def isAsynchronous: Boolean = true
		
		override def delay: Span[FiniteDuration] = by
		
		override def self: Delayed = this
		
		override def *(mod: Double): Delayed =
			map { d => (d * mod).finite.getOrElse { throw new IllegalArgumentException(s"Scaling duration by $mod") } }
		
		override def +(other: FiniteDuration): Delayed = map { _ + other }
		
		
		// OTHER    ----------------------------
		
		/**
		  * @param delay A new minimum delay
		  * @return A copy of this approach with the specified minimum delay
		  */
		def withMinDelay(delay: FiniteDuration) = Delayed(Span(delay, delay max maxDelay))
		/**
		  * @param delay A new maximum delay
		  * @return A copy of this approach with the specified maximum delay
		  */
		def withMaxDelay(delay: FiniteDuration) = Delayed(Span(delay min minDelay, delay))
		
		/**
		  * @param f A mapping function
		  * @return A copy of this approach with mapped delay
		  */
		def map(f: FiniteDuration => FiniteDuration) = Delayed(by.mapEnds(f).ascending)
	}
}
