package utopia.flow.datastructure.mutable

import scala.concurrent.duration.Duration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

object ResettableLazy
{
	/**
	  * Creates a new lazily initialized wrapper
	  * @param make A function for generating the wrapped item when it is requested (may be called multiple times)
	  * @tparam A Type of wrapped item
	  * @return A new lazy wrapper
	  */
	def apply[A](make: => A) = new ResettableLazy[A](make)
	
	/**
	  * Creates a listenable lazily initialized wrapper
	  * @param make A function for generating the wrapped item on request
	  * @tparam A Type of the wrapped item
	  * @return A new lazy container with events
	  */
	def listenable[A](make: => A) = ListenableResettableLazy(make)
	
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
}

/**
  * This lazily initialized container allows one to request a new value initialization
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
class ResettableLazy[A](generator: => A) extends ResettableLazyLike[A]
{
	// ATTRIBUTES	---------------------------
	
	private var _value: Option[A] = None
	
	
	// IMPLEMENTED	---------------------------
	
	override def reset() = _value = None
	
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
