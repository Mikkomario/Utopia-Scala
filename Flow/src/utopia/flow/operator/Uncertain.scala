package utopia.flow.operator

import utopia.flow.util.UncertainBoolean
import utopia.flow.util.UncertainBoolean.CertainBoolean

/**
 * Common trait for classes where the exact value is not always known
 * @author Mikko Hilpinen
 * @since 18.8.2023, v2.2
  * @tparam A Type of the wrapped value, when certainly known
 */
trait Uncertain[A]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return Exactly known value. None if not known exactly.
	 */
	def exact: Option[A]
	
	/**
	  * @param v A value
	  * @return Whether this item might match exactly the specified value.
	  */
	def mayBe(v: A): Boolean
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Whether this item represents an exact / precise value
	  */
	def isExact = exact.isDefined
	/**
	  * @return Whether this item represents an unknown or an inexact value
	  */
	def nonExact = !isExact
	
	
	// OTHER    --------------------------
	
	/**
	  * @param v A value
	  * @tparam B Type of the specified value
	  * @return Whether this item is known to exactly match the specified value
	  */
	def isCertainlyExactly[B >: A](v: B) = exact.contains(v)
	/**
	  * @param v A value
	  * @return Whether this item is known to not match the specified value
	  */
	def isCertainlyNot(v: A) = !mayBe(v)
	
	/**
	 * @param default Value returned in uncertain cases (call-by-name)
	 * @return This value if known, otherwise the default value
	 */
	def getOrElse[B >: A](default: => B) = exact.getOrElse(default)
	
	/**
	  * @param v A value
	  * @return Whether this value equals the specified value.
	  *         This result may be uncertain.
	  */
	def ==(v: A): UncertainBoolean = {
		if (isCertainlyExactly(v))
			CertainBoolean(true)
		else if (mayBe(v))
			UncertainBoolean
		else
			CertainBoolean(false)
	}
	/**
	  * @param v A value
	  * @return Whether this value does not equal the specified value.
	  *         This result may be uncertain.
	  */
	def !=(v: A): UncertainBoolean = {
		if (mayBe(v)) {
			if (isCertainlyExactly(v))
				CertainBoolean(false)
			else
				UncertainBoolean
		}
		else
			CertainBoolean(true)
	}
}

