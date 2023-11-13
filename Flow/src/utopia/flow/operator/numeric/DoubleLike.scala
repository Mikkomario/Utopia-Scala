package utopia.flow.operator.numeric

import utopia.flow.operator.HasLength
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.LinearScalable
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.{Sign, SignOrZero, SignedOrZero}

import scala.language.implicitConversions

object DoubleLike
{
	// IMPLICIT ----------------------------
	
	implicit def unwrapDouble(d: DoubleWrapper): Double = d.d
	
	implicit class DoubleWrapper(val d: Double) extends AnyVal with DoubleLike[DoubleWrapper]
	{
		override def self = this
		
		override def sign: SignOrZero = Sign.of(d)
		override def length = d
		
		override def zero = 0.0
		
		override def compareTo(o: DoubleWrapper) = d.compareTo(o.d)
		
		override def *(mod: Double) = d * mod
		override def +(other: DoubleWrapper) = d + other.d
	}
}

/**
  * A common trait for items which support linear scaling and other numeric operators (+, -)
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait DoubleLike[Repr] extends Any with LinearScalable[Repr] with SelfCombinable[Repr] with SignedOrZero[Repr]
	with SelfComparable[Repr] with HasLength