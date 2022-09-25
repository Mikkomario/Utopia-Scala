package utopia.flow.operator

import scala.language.implicitConversions

object DoubleLike
{
	implicit def unwrapDouble(d: DoubleWrapper): Double = d.d
	
	implicit class DoubleWrapper(val d: Double) extends AnyVal with DoubleLike[DoubleWrapper]
	{
		override def isPositive = d > 0
		
		override def isZero = d == 0
		
		override def length = d
		
		override def *(mod: Double) = d * mod
		
		override def +(other: DoubleWrapper) = d + other.d
		
		override def repr = this
		
		override def zero = 0.0
		
		override def compareTo(o: DoubleWrapper) = d.compareTo(o.d)
	}
}

/**
  * A common trait for items which support linear scaling and other numeric operators (+, -)
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait DoubleLike[Repr] extends Any with LinearScalable[Repr] with Combinable[Repr, Repr] with SignedOrZero[Repr]
	with SelfComparable[Repr] with LinearMeasurable