package utopia.flow.operator.numeric

import utopia.flow.operator.HasLength
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.Multiplicable
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.{Sign, SignOrZero, SignedOrZero}

import scala.language.implicitConversions

object IntLike
{
	implicit def unwrapInt(i: IntWrapper): Int = i.i
	
	implicit class IntWrapper(val i: Int) extends AnyVal with IntLike[IntWrapper]
	{
		override def self = this
		
		override def sign: SignOrZero = Sign.of(i)
		override def length = i
		
		override def zero = 0
		
		override def compareTo(o: IntWrapper) = i - o.i
		
		override def *(mod: Int) = i * mod
		override def +(other: IntWrapper) = i + other.i
	}
}

/**
  * A common trait for values which behave like integer numbers (support +, - and * but not linear scaling or /)
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait IntLike[Repr] extends Any with Multiplicable[Repr] with HasLength with SelfCombinable[Repr]
	with SelfComparable[Repr] with SignedOrZero[Repr]