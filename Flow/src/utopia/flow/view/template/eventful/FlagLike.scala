package utopia.flow.view.template.eventful

import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue}

object FlagLike
{
	// IMPLICIT ------------------
	
	// Wraps any Changing[Boolean] into a more specific FlagLike
	implicit def wrap(c: Changing[Boolean]): FlagLike = new FlagLikeWrapper(c)
	
	
	// NESTED   ------------------
	
	private class FlagLikeWrapper(override protected val wrapped: Changing[Boolean])
		extends FlagLike with ChangingWrapper[Boolean]
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
	def unary_! = fixedValue match {
		case Some(fixed) => if (fixed) AlwaysFalse else AlwaysTrue
		case None => map { !_ }
	}
	
	
	// OTHER	-----------------
	
	/**
	  * @param other Another flag
	  * @return A flag that contains true when both of these flags contain true
	  */
	def &&(other: Changing[Boolean]) = {
		// If one of the pointers is always false, returns always false
		// If one of the pointers is always true, returns the other pointer
		// If both are changing, returns a combination of these pointers
		fixedValue match {
			case Some(fixed) => if (fixed) other else AlwaysFalse
			case None =>
				other.fixedValue match {
					case Some(fixed) => if (fixed) this else AlwaysFalse
					case None => mergeWith(other) { _ && _ }
				}
		}
	}
	/**
	  * @param other Another flag
	  * @return A flag that contains true when either one of these flags contains true
	  */
	def ||(other: Changing[Boolean]) = {
		// If one of the pointers is always false, returns the other pointer
		// If one of the pointers is always true, returns always true
		// If both are changing, returns a combination of these pointers
		fixedValue match {
			case Some(fixed) => if (fixed) AlwaysTrue else other
			case None =>
				other.fixedValue match {
					case Some(fixed) => if (fixed) AlwaysTrue else this
					case None => mergeWith(other) { _ || _ }
				}
		}
	}
}
