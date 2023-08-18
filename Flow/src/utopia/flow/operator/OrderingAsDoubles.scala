package utopia.flow.operator

import scala.annotation.unchecked.uncheckedVariance

/**
  * A wrapper that provides contravariant ordering for numeric classes
  * by converting the numbers to doubles before comparing them
  * @author Mikko Hilpinen
  * @since 18.8.2023, v2.2
  */
class OrderingAsDoubles[-A](n: Numeric[A]) extends Ordering[A @uncheckedVariance]
{
	override def compare(x: A, y: A): Int = n.toDouble(x).compareTo(n.toDouble(y))
}
