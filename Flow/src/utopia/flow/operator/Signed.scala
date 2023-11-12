package utopia.flow.operator

/**
  * A common trait for items which can be either positive or negative, but not necessarily zero
  * @author Mikko Hilpinen
  * @since 21.9.2021, v1.12
  */
trait Signed[+Repr] extends Any with HasSign with Reversible[Repr]
{
	// COMPUTED    ----------------------
	
	/**
	  * @return True if this item is not positive. False otherwise.
	  */
	@deprecated("Please call .sign.isNotPositive instead", "v2.2")
	def nonPositive = !isPositive
	/**
	  * @return True if this item is not negative. False otherwise.
	  */
	@deprecated("Please call .sign.isNotNegative instead", "v2.2")
	def nonNegative = !isNegative
	
	/**
	  * @return Absolute value of this item
	  */
	def abs = if (sign.isNegative) -this else self
	/**
	  * @return Negative absolute value of this item
	  */
	def negativeAbs = if (sign.isPositive) -this else self
	
	/**
	  * @return Some(this) if positive, None otherwise
	  */
	def ifPositive = if (sign.isPositive) Some(self) else None
	/**
	  * @return Some(this) if negative, None otherwise
	  */
	def ifNegative = if (sign.isNegative) Some(self) else None
	
	/**
	  * @return Some(this) if not positive, None otherwise
	  */
	def notPositive = if (sign.isNotPositive) Some(self) else None
	/**
	  * @return Some(this) if not negative, None otherwise
	  */
	def notNegative = if (sign.isNotNegative) Some(self) else None
	
	
	// OTHER    --------------------------
	
	/**
	  * @param sign A sign (+ or -)
	  * @return A copy of this item with that sign. Returns this if zero.
	  */
	def withSign(sign: Sign) = if (is(sign.opposite)) -this else self
	/**
	  * @param sign A sign
	  * @return Some(this) if this item is of that sign. None otherwise.
	  */
	def ifHasSign(sign: SignOrZero) = if (is(sign)) Some(self) else None
	
	/**
	  * @param default Value to return if this item is not positive
	  * @tparam B Type of default item
	  * @return This item if positive, otherwise the specified default
	  */
	def positiveOrElse[B >: Repr](default: => B) = if (sign.isPositive) self else default
	/**
	  * @param default Value to return if this item is not negative
	  * @tparam B Type of default item
	  * @return This item if negative, otherwise the specified default
	  */
	def negativeOrElse[B >: Repr](default: => B) = if (sign.isNegative) self else default
	/**
	  * @param sign Accepted sign
	  * @param default Value specified if this item is not of that sign (call-by-name)
	  * @tparam B Type of default value
	  * @return This item if of that sign, otherwise the default value
	  */
	def withSignOrElse[B >: Repr](sign: SignOrZero, default: => B) = if (is(sign)) self else default
	
	/**
	  * @param f A mapping function to apply if this item is positive
	  * @tparam B Type of mapping result
	  * @return Mapped copy of this item if this was positive, otherwise this item as is
	  */
	def mapIfPositive[B >: Repr](f: Repr => B) = if (sign.isPositive) f(self) else self
	/**
	  * @param f A mapping function to apply if this item is negative
	  * @tparam B Type of mapping result
	  * @return Mapped copy of this item if this was negative, otherwise this item as is
	  */
	def mapIfNegative[B >: Repr](f: Repr => B) = if (sign.isNegative) f(self) else self
	/**
	  * @param sign Sign for which the mapping function applies
	  * @param f A mapping function applied for this item, if of the specified sign
	  * @tparam B Type of mapping result
	  * @return A mapped copy of this item, if this was of the specified sign. This item otherwise.
	  */
	def mapIfHasSign[B >: Repr](sign: SignOrZero)(f: Repr => B) = if (is(sign)) f(self) else self
}
