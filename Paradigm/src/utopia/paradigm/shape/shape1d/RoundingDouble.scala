package utopia.paradigm.shape.shape1d

import utopia.flow.operator.{CanBeAboutZero, DoubleLike, EqualsFunction}

import scala.language.implicitConversions

object RoundingDouble
{
	// ATTRIBUTES   --------------------
	
	/**
	  * A zero value
	  */
	val zero = apply(0.0)
	
	/**
	  * Numeric implementation for this class
	  */
	implicit val numeric: Fractional[RoundingDouble] = NumericRoundingDouble
	/**
	  * Equality function applicable to this class. Rounds before testing equality.
	  */
	implicit val equals: EqualsFunction[RoundingDouble] = _.int == _.int
	
	
	// IMPLICIT ------------------------
	
	implicit def implicitRounding(d: Double): RoundingDouble = apply(d)
	implicit def implicitlyDouble(d: RoundingDouble): Double = d.double
	implicit def implicitlyInt(d: RoundingDouble): Int = d.int
	
	
	// NESTED   ------------------------
	
	private object NumericRoundingDouble extends Fractional[RoundingDouble]
	{
		override def plus(x: RoundingDouble, y: RoundingDouble): RoundingDouble = x + y
		override def minus(x: RoundingDouble, y: RoundingDouble): RoundingDouble = RoundingDouble(x.wrapped - y.wrapped)
		override def times(x: RoundingDouble, y: RoundingDouble): RoundingDouble = x * y
		override def div(x: RoundingDouble, y: RoundingDouble): RoundingDouble = RoundingDouble(x.wrapped / y.wrapped)
		override def negate(x: RoundingDouble): RoundingDouble = RoundingDouble(-x.wrapped)
		
		override def fromInt(x: Int): RoundingDouble = RoundingDouble(x)
		override def parseString(str: String): Option[RoundingDouble] = str.toDoubleOption.map { RoundingDouble(_) }
		
		override def toInt(x: RoundingDouble): Int = x.int
		override def toLong(x: RoundingDouble): Long = x.int.toLong
		override def toFloat(x: RoundingDouble): Float = x.int.toFloat
		override def toDouble(x: RoundingDouble): Double = x.double
		
		override def compare(x: RoundingDouble, y: RoundingDouble): Int = x.compareTo(y)
	}
}

/**
  * A wrapper for double numbers that externally rounds the value to the nearest integer,
  * but internally preserves the non-rounded number in order to add precision to additional calculations.
  * @author Mikko Hilpinen
  * @since 28.7.2023, v1.3.1
  */
case class RoundingDouble(wrapped: Double)
	extends DoubleLike[RoundingDouble] with CanBeAboutZero[Double, RoundingDouble]
{
	// ATTRIBUTES   ------------------------
	
	lazy val int = wrapped.round.toInt
	lazy val double = int.toDouble
	
	
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	override def zero = RoundingDouble.zero
	override def isZero = wrapped == 0.0
	override def isAboutZero: Boolean = int == 0
	override def isPositive = int > 0
	
	override def length = double
	
	override def compareTo(o: RoundingDouble) = wrapped.compareTo(o.wrapped)
	
	override def *(mod: Double) = RoundingDouble(wrapped * mod)
	override def +(other: RoundingDouble) = RoundingDouble(wrapped + other.wrapped)
	
	override def ~==(other: Double): Boolean = int == other.round
	
	
	// OTHER    --------------------------
	
	/**
	  * Approximately compares this number with another rounded number
	  * @param other Another rounded number
	  * @return Whether these numbers round to the same number
	  */
	def ~==(other: RoundingDouble) = int == other.int
}
