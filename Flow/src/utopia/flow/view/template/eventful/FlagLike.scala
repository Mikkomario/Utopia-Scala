package utopia.flow.view.template.eventful

import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue}

import scala.language.implicitConversions

object FlagLike
{
	// IMPLICIT ------------------
	
	// Wraps any Changing[Boolean] into a more specific FlagLike
	implicit def wrap(c: Changing[Boolean]): FlagLike = c match {
		case f: FlagLike => f
		case o => new FlagLikeWrapper(o)
	}
	
	
	// NESTED   ------------------
	
	private class FlagLikeWrapper(override protected val wrapped: Changing[Boolean])
		extends FlagLike with ChangingWrapper[Boolean]
	{
		//noinspection PostfixUnaryOperation
		override lazy val unary_! = super.unary_!
	}
}

/**
  * A common trait for items which resemble a boolean flag
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait FlagLike extends Any with Changing[Boolean]
{
	// COMPUTED	-----------------
	
	/**
	  * @return Whether this flag has been set
	  */
	def isSet = value
	/**
	  * @return Whether this flag hasn't been set yet
	  */
	def isNotSet = !isSet
	
	/**
	  * @return Whether this flag will always remain true
	  */
	def isAlwaysTrue = existsFixed { b => b }
	/**
	  * @return Whether this flag will always remain false
	  */
	def isAlwaysFalse = existsFixed { !_ }
	
	/**
	  * @return A reversed copy of this flag
	  */
	def unary_! : FlagLike = fixedValue match {
		case Some(fixed) => if (fixed) AlwaysFalse else AlwaysTrue
		case None => lightMap { !_ }
	}
	
	/**
	  * @return Future that resolves when this flag is set
	  */
	def future = findMapFuture { if (_) Some(()) else None }
	/**
	  * @return Future that resolves when this flag is set the next time
	  */
	def nextFuture = findMapNextFuture { if (_) Some(()) else None }
	
	
	// OTHER	-----------------
	
	/**
	  * @param other Another flag
	  * @return A flag that contains true when both of these flags contain true
	  */
	def &&(other: Changing[Boolean]): FlagLike = {
		// If one of the pointers is always false, returns always false
		// If one of the pointers is always true, returns the other pointer
		// If both are changing, returns a combination of these pointers
		fixedValue match {
			case Some(fixed) => if (fixed) other else AlwaysFalse
			case None =>
				other.fixedValue match {
					case Some(fixed) => if (fixed) this else AlwaysFalse
					case None =>
						// Stops merging if either pointer gets stuck at false
						withState.lightMergeWithUntil(other.withState) { _ && _ } {
							(a, b, _) => a.containsFinal(false) || b.containsFinal(false) }
				}
		}
	}
	/**
	  * @param other Another flag
	  * @return A flag that contains true when either one of these flags contains true
	  */
	def ||(other: Changing[Boolean]): FlagLike = {
		// If one of the pointers is always false, returns the other pointer
		// If one of the pointers is always true, returns always true
		// If both are changing, returns a combination of these pointers
		fixedValue match {
			case Some(fixed) => if (fixed) AlwaysTrue else other
			case None =>
				other.fixedValue match {
					case Some(fixed) => if (fixed) AlwaysTrue else this
					case None =>
						// Stops merging if either pointer gets stuck at true
						withState.lightMergeWithUntil(other.withState) { _ || _ } {
							(a, b, _) => a.containsFinal(true) || b.containsFinal(true) }
				}
		}
	}
	
	/**
	  * Performs the specified function once this flag is set.
	  * If this flag is already set, calls the function immediately.
	  * @param f A function to call when this flag is set (will be called 0 or 1 times only)
	  * @tparam U Arbitrary function result type
	  */
	def onceSet[U](f: => U) = addListenerAndSimulateEvent(false) { e =>
		if (e.newValue) {
			f
			Detach
		}
		else
			Continue
	}
	/**
	  * Performs the specified function once this flag is reset / not set.
	  * If this flag is not set at this time, calls the function immediately.
	  * @param f A function to call once this flag is not set (will be called 0 or 1 times only)
	  * @tparam U Arbitrary function result type
	  */
	def onceNotSet[U](f: => U) = addListenerAndSimulateEvent(true) { e =>
		if (e.newValue)
			Continue
		else {
			f
			Detach
		}
	}
	/**
	  * Performs the specified function once this flag is set.
	  * If this flag is already set, will only call the specified function after this flag has been
	  * reset and then set again.
	  * If this is not a resettable flag and has been set, the specified function will never get called.
	  * @param f A function to call when this flag is set (will be called 0 or 1 times only)
	  * @tparam U Arbitrary function result type
	  */
	def whenNextSet[U](f: => U) = addListener { e =>
		if (e.newValue) {
			f
			Detach
		}
		else
			Continue
	}
}
