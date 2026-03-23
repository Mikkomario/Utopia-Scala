package utopia.echo.model.tokenization

import utopia.flow.collection.immutable.Single
import utopia.flow.operator.combine.{Combinable, LinearScalable, Subtractable}
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.operator.ordering.RichComparable
import utopia.flow.operator.sign.{Sign, SignOrZero, SignedOrZero}
import utopia.flow.operator.{MayBeZero, Reversible}
import utopia.flow.util.NumberExtensions._
import utopia.flow.view.immutable.View

import scala.language.implicitConversions

/**
 * Common trait for token count calculations & estimations
 * @author Mikko Hilpinen
 * @since 22.03.2026, v1.6
 */
trait TokenCountLike[+Repr]
	extends View[Int] with MayBeZero[Repr] with Combinable[TokenCount, Repr] with Reversible[Repr]
		with Subtractable[TokenCount, Repr] with LinearScalable[Repr] with EqualsBy with RichComparable[TokenCount]
		with SignedOrZero[Repr]
{
	// ABSTRACT ------------------------------
	
	protected def withValue(value: Int): Repr
	
	
	// IMPLEMENTED  --------------------------
	
	override def sign: SignOrZero = Sign.of(value)
	override def isZero: Boolean = value == 0
	override def zero: Repr = withValue(0)
	
	def positiveOrZero = if (isNegative) zero else self
	def negativeOrZero = if (isPositive) zero else self
	
	override def unary_- = withValue(-value)
	
	override protected def equalsProperties: IterableOnce[Any] = Single(value)
	
	override def +(other: TokenCount): Repr = this + other.value
	override def -(other: TokenCount): Repr = this + (-other)
	override def *(mod: Double): Repr = withValue((value * mod).round.toInt)
	
	override def toString = {
		if (value >= 1000000)
			s"${ (value / 1000000.0).roundDecimals(1) } M"
		else if (value >= 10000)
			s"${ (value / 1000.0).round } K"
		else if (value >= 1000)
			s"${ (value / 1000.0).roundDecimals(1) } K"
		else
			value.toString
	}
	
	override def compareTo(o: TokenCount) = value - o.value
	
	
	// OTHER    ----------------------------
	
	def +(amount: Int) = withValue(value + amount)
	def -(amount: Int) = this + (-amount)
	
	def /(other: TokenCount) = value / other.value.toDouble
}