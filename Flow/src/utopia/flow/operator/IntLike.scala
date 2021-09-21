package utopia.flow.operator

import utopia.flow.util.SelfComparable

import scala.language.implicitConversions

object IntLike
{
	implicit def unwrapInt(i: IntWrapper): Int = i.i
	
	implicit class IntWrapper(val i: Int) extends AnyVal with IntLike[IntWrapper]
	{
		override def length = i
		
		override def *(mod: Int) = i * mod
		
		override def +(other: IntWrapper) = i + other.i
		
		override def repr = this
		
		override def isZero = i == 0
		
		override def isPositive = i > 0
		
		override protected def zero = 0
		
		override def compareTo(o: IntWrapper) = i - o.i
	}
}

/**
  * A common trait for values which behave like integer numbers (support +, - and * but not linear scaling or /)
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait IntLike[Repr] extends Any with Multiplicable[Repr] with LinearMeasurable with Combinable[Repr, Repr]
	with SelfComparable[Repr] with SignedOrZero[Repr]