package utopia.echo.model.tokenization

import utopia.flow.operator.sign.Sign
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.StringExtensions._

import scala.language.implicitConversions

object TokenCount
{
	// COMPUTED ------------------------
	
	def zero: TokenCount = ZeroTokens
	
	
	// IMPLICIT ------------------------
	
	implicit def numeric: Numeric[TokenCount] = TokenCountIsNumeric
	
	/**
	 * @param tokens Number of tokens
	 * @return A token count matching that amount of tokens
	 */
	implicit def apply(tokens: Int): TokenCount = new _TokenCount(tokens)
	
	
	// NESTED   ------------------------
	
	/**
	 * Numeric implementation for [[TokenCount]]
	 */
	object TokenCountIsNumeric extends Numeric[TokenCount]
	{
		override def plus(x: TokenCount, y: TokenCount): TokenCount = x + y
		override def minus(x: TokenCount, y: TokenCount): TokenCount = x - y
		
		override def times(x: TokenCount, y: TokenCount): TokenCount = x * y.value
		
		override def negate(x: TokenCount): TokenCount = -x
		
		override def fromInt(x: Int): TokenCount = apply(x)
		override def parseString(str: String): Option[TokenCount] = {
			val (valuePart, unitPart) = str.splitAtFirst(" ").toTuple
			if (unitPart.length <= 1) {
				val multiplier = unitPart.toUpperCase match {
					case "" => Some(1)
					case "K" => Some(1000)
					case "M" => Some(1000000)
				}
				multiplier.flatMap { multiplier => valuePart.int.map { value => apply(value * multiplier) } }
			}
			else
				None
		}
		
		override def toInt(x: TokenCount): Int = x.value
		override def toLong(x: TokenCount): Long = toInt(x)
		override def toFloat(x: TokenCount): Float = toInt(x).toFloat
		override def toDouble(x: TokenCount): Double = toInt(x)
		
		override def compare(x: TokenCount, y: TokenCount): Int = x.compareTo(y)
	}
	
	private object ZeroTokens extends TokenCount
	{
		override val value: Int = 0
		
		override def self: TokenCount = this
		
		override def isZero = true
		override def zero = this
		
		override def unary_- = this
		
		override def +(other: TokenCount) = other
		override def *(mod: Double) = this
		override def *(sign: Sign) = this
		override def /(div: Double) = this
		
		override protected def withValue(value: Int): TokenCount = new _TokenCount(value)
	}
	
	private class _TokenCount(override val value: Int) extends TokenCount
	{
		override def self: TokenCount = this
		override def zero = ZeroTokens
		
		override protected def withValue(value: Int): TokenCount = new _TokenCount(value)
	}
}

/**
 * Common trait for individual token counts & estimates
 * @author Mikko Hilpinen
 * @since 22.03.2026, v1.6
 */
trait TokenCount extends TokenCountLike[TokenCount]
{
	def asEstimate = EstimatedTokenCount.from(self)
	
	def min(other: TokenCount) = atMost(other)
	def max(other: TokenCount) = atLeast(other)
	
	def atLeast(other: TokenCount) = if (this >= other) this else other
	def atMost(other: TokenCount) = if (this <= other) this else other
}