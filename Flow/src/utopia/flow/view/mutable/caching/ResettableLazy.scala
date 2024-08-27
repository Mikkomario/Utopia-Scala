package utopia.flow.view.mutable.caching

import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Resettable

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object ResettableLazy
{
	// OTHER    ----------------------
	
	/**
	  * Creates a new lazily initialized wrapper
	  * @param make A function for generating the wrapped item when it is requested (maybe called multiple times)
	  * @tparam A Type of wrapped item
	  * @return A new lazy wrapper
	  */
	def apply[A](make: => A): ResettableLazy[A] = new _ResettableLazy[A](make)
	
	/**
	  * Creates a listenable lazily initialized wrapper
	  * @param make A function for generating the wrapped item on request
	  * @param log Logging implementation for recording failures thrown by listeners
	  * @tparam A Type of the wrapped item
	  * @return A new lazy container with events
	  */
	def listenable[A](make: => A)(implicit log: Logger) = ListenableResettableLazy(make)
	
	/**
	  * @param threshold Time threshold after which this lazy is automatically reset
	  * @param make A function for generating a new value when one is requested
	  * @param exc Implicit execution context for scheduling resets
	  * @tparam A Type of wrapped item
	  * @return A new lazy container with automated reset
	  */
	def expiringAfter[A](threshold: Duration)(make: => A)(implicit exc: ExecutionContext, logger: Logger) =
		threshold.finite match {
			case Some(finite) => ExpiringLazy.after(finite)(make)
			case None => apply(make)
		}
	
	
	// NESTED   ----------------------
	
	private class _ResettableLazy[A](generator: => A) extends ResettableLazy[A]
	{
		// ATTRIBUTES	---------------------------
		
		private var _value: Option[A] = None
		
		
		// IMPLEMENTED	---------------------------
		
		override def reset() = {
			if (_value.isDefined) {
				_value = None
				true
			}
			else
				false
		}
		
		override def current = _value
		
		override def value = _value match
		{
			case Some(value) => value
			case None =>
				val newValue = generator
				_value = Some(newValue)
				newValue
		}
	}
}

/**
  * A common trait for lazily initialized containers which allow the wrapped value to be reset, so that it is generated
  * possibly multiple times
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
trait ResettableLazy[+A] extends Lazy[A] with Resettable
{
	// COMPUTED ----------------------
	
	/**
	 * @return An infinite iterator that first returns the first value in this lazy,
	 *         and then continues providing new values (using newValue())
	 */
	def resettingValueIterator = valueIterator ++ Iterator.continually { newValue() }
	
	
	// OTHER	----------------------
	
	/**
	  * Retrieves a value from this lazy and resets it afterwards
	  * @return The value in this lazy before the reset
	  */
	def pop() = {
		val result = value
		reset()
		result
	}
	/**
	  * Retrieves a value from this lazy, if one has already been initialized, and resets it afterwards (if necessary)
	  * @return The value in this lazy before the reset. None if this lazy didn't have an initialized value.
	  */
	def popCurrent() = {
		val result = current
		if (result.nonEmpty)
			reset()
		result
	}
	
	/**
	  * Resets this lazy instance and immediately requests a new value
	  * @return The newly generated and stored value
	  */
	def newValue() = {
		reset()
		value
	}
	
	/**
	  * Keeps the current value only if it fulfills the specified condition
	  * @param keepCondition A condition for the currently cached value to keep
	  * @return Whether the state of this lazy container changed
	  */
	def filter(keepCondition: A => Boolean) = filterNot { c => !keepCondition(c) }
	/**
	  * Clears the current value if it matches the specified condition
	  * @param resetCondition A condition on which the cached item is cleared
	  * @return Whether the state of this lazy container changed
	  */
	def filterNot(resetCondition: A => Boolean) = {
		if (current.exists(resetCondition))
			reset()
		else
			false
	}
}
