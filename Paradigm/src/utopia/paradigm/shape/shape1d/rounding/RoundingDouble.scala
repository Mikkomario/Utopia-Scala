package utopia.paradigm.shape.shape1d.rounding

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.RoundingFunction.{Ceil, Floor, Round}
import utopia.flow.operator.{CanBeAboutZero, DoubleLike, EqualsFunction, RoundingFunction, Sign, SignOrZero}
import utopia.flow.view.immutable.View

import scala.language.implicitConversions

object RoundingDouble
{
	// ATTRIBUTES   --------------------
	
	/**
	  * A zero value
	  */
	val zero = apply(0.0)
	/**
	  * A unit (1) value
	  */
	val unit = apply(1.0)
	
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
		override def minus(x: RoundingDouble, y: RoundingDouble): RoundingDouble = RoundingDouble(x.value - y.value)
		override def times(x: RoundingDouble, y: RoundingDouble): RoundingDouble = x * y
		override def div(x: RoundingDouble, y: RoundingDouble): RoundingDouble = RoundingDouble(x.value / y.value)
		override def negate(x: RoundingDouble): RoundingDouble = RoundingDouble(-x.value)
		
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
case class RoundingDouble(value: Double, logic: RoundingFunction = Round)
	extends DoubleLike[RoundingDouble] with CanBeAboutZero[Double, RoundingDouble] with ValueConvertible with View[Double]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * This number as an integer
	  */
	lazy val int = logic(value).toInt
	/**
	  * This number as a double (rounded)
	  */
	lazy val double = int.toDouble
	
	override lazy val sign: SignOrZero = Sign.of(value)
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Copy of this number where rounding is to the nearest integer
	  */
	def round = withLogic(Round)
	/**
	  * @return Copy of this number where the rounding is to the next full integer
	  */
	def ceil = withLogic(Ceil)
	/**
	  * @return Copy of this number where decimal places are removed
	  */
	def floor = withLogic(Floor)
	
	
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	override def zero = RoundingDouble.zero
	override def isAboutZero: Boolean = int == 0
	
	override def length = double
	
	override implicit def toValue: Value = int
	override def toString = int.toString
	
	override def compareTo(o: RoundingDouble) = {
		val intCompare = int - o.int
		if (intCompare == 0)
			value.compareTo(o.value)
		else
			intCompare
	}
	
	override def +(other: RoundingDouble) = {
		val resultingLogic = logicBetween(other)
		RoundingDouble(value + other.value, resultingLogic)
	}
	override def *(mod: Double) = RoundingDouble(value * mod)
	override def /(div: Double) = copy(value / div)
	
	override def ~==(other: Double): Boolean = int == other.round
	
	
	// OTHER    --------------------------
	
	def +(d: Double) = copy(value + d)
	def -(d: Double) = copy(value - d)
	def -(other: RoundingDouble) = RoundingDouble(value - other.value, logicBetween(other))
	def *(mod: RoundingDouble) = RoundingDouble(value * mod.value, logicBetween(mod))
	def /(div: RoundingDouble) = copy(value / div.value)
	
	/**
	  * @param logic Rounding logic to apply
	  * @return Copy of this number using that rounding logic
	  */
	def withLogic(logic: RoundingFunction) = if (logic == this.logic) this else copy(logic = logic)
	
	/**
	  * Approximately compares this number with another rounded number
	  * @param other Another rounded number
	  * @return Whether these numbers round to the same number
	  */
	def ~==(other: RoundingDouble) = int == other.int
	
	private def logicBetween(other: RoundingDouble) = {
		if (logic == other.logic)
			logic
		else if ((value - double).abs >= (other.value - other.double).abs)
			logic
		else
			other.logic
	}
}
