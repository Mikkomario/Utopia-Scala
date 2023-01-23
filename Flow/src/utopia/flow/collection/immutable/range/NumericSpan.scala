package utopia.flow.collection.immutable.range

import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator._

import scala.language.implicitConversions

object NumericSpan
{
	// OTHER    -------------------------
	
	implicit def rangeToSpan(r: Range.Inclusive): NumericSpan[Int] = apply(r.start, r.end, r.step)
	
	/**
	  * @param start The starting point of this span
	  * @param end The inclusive end point of this span
	  * @param n A numeric implementation
	  * @tparam N Type of numeric value used
	  * @return A new numeric span
	  */
	def apply[N](start: N, end: N)(implicit n: Numeric[N]): NumericSpan[N] = apply(start, end, n.one)
	/**
	  * @param start The starting point of this span
	  * @param end   The inclusive end point of this span
	  * @param step Length of a step taken at each iteration along this span.
	  *             Sign of this parameter doesn't matter (absolute value will be used)
	  * @param n     A numeric implementation
	  * @tparam N Type of numeric value used
	  * @return A new numeric span
	  */
	def apply[N](start: N, end: N, step: N)(implicit n: Numeric[N]): NumericSpan[N] = new _NumericSpan(start, end, step)
	
	
	// IMPLEMENTED  ---------------------
	
	class _NumericSpan[N](override val start: N, override val end: N, _step: N)
	                     (override implicit val n: Numeric[N])
		extends NumericSpan[N] with EqualsBy
	{
		// ATTRIBUTES   -------------------------
		
		override lazy val step = n.abs(_step)
		
		/**
		  * The length of this span, which may be negative
		  */
		override lazy val length = super.length
		
		
		// IMPLEMENTED  -------------------------
		
		override protected def equalsProperties = Vector(start, end, step)
	}
}

/**
  * An inclusive range implementation using numeric instances.
  * Resembles NumericRange
  * @author Mikko Hilpinen
  * @since 17.12.2022, v2.0
  */
trait NumericSpan[N]
	extends IterableSpan[N] with SpanLike[N, NumericSpan[N]] with Combinable[N, NumericSpan[N]]
		with Reversible[NumericSpan[N]] with Scalable[N, NumericSpan[N]]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Distance advanced with each step along this span.
	  *         Absolute value, i.e. always positive, even when this span is descending in nature.
	  */
	def step: N
	
	/**
	  * @return Numeric implementation for the points along this span
	  */
	def n: Numeric[N]
	
	
	// COMPUTED   -------------------------
	
	/**
	  * The length of this span, which may be negative
	  */
	def length = n.minus(end, start)
	
	
	// IMPLEMENTED  -------------------------
	
	override def self = this
	
	override def unary_- = withEnds(n.negate(start), n.negate(end))
	
	override implicit def ordering: Ordering[N] = n
	
	override def withEnds(start: N, end: N) = NumericSpan(start, end, step)(n)
	
	override protected def traverse(from: N, direction: Sign) = n.plus(from, direction match {
		case Positive => step
		case Negative => n.negate(step)
	})
	
	override def +(other: N) = shiftedBy(other)
	
	override def *(mod: N) = NumericSpan(n.times(start, mod), n.times(end, mod), n.times(step, mod))(n)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param step Length of step taken at each iteration.
	  *             NB: The sign of this parameter is ignored and an absolute value is used instead.
	  * @return A copy of this span with the specified step -property
	  */
	def by(step: N) = NumericSpan(start, end, step)(n)
	
	/**
	  * @param distance A distance to move this span
	  * @return A copy of this span where both the start and the end points have been moved the specified distance
	  */
	def shiftedBy(distance: N) = _shiftedBy(distance)(n.plus)
	
	/**
	  * @param length New length to assign to this span
	  * @return A copy of this span with the same starting point and the new length
	  */
	def withLength(length: N) = _withLength(length)(n.plus)
	/**
	  * @param maxLength Largest allowed length
	  * @return A copy of this span with length equal to or smaller than the specified maximum length.
	  *         The starting point of this span is preserved.
	  */
	def withMaxLength(maxLength: N) = _withMaxLength(maxLength)(n.plus)
	/**
	  * @param minLength Smallest allowed length
	  * @return A copy of this span with length equal to or larger than the specified minimum length.
	  *         The starting point of this span is preserved.
	  */
	def withMinLength(minLength: N) = _withMinLength(minLength)(n.plus)
	
	/**
	  * Moves this span so that it either:
	  * a) Lies completely within the specified span, or
	  * b) Covers the specified span entirely
	  * The applied movement is minimized
	  * @param other Another span
	  * @return A copy of this span that fulfills a condition specified above
	  */
	def shiftedInto(other: HasInclusiveEnds[N]) = _shiftedInto(other)(n.plus)(n.minus)
}
