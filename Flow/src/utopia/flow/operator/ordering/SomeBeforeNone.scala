package utopia.flow.operator.ordering

import scala.annotation.unused
import scala.language.implicitConversions

object SomeBeforeNone
{
	// IMPLICIT ---------------------
	
	implicit def objectToOrdering[A](@unused o: SomeBeforeNone.type)
	                                (implicit ord: Ordering[A]): Ordering[Option[A]] =
		apply()
	
	
	// OTHER    ---------------------
	
	/**
	  * @param ord Implicit ordering to use for defined values
	  * @tparam A Type of the defined values
	  * @return Ordering that uses the specified ordering, but places None at the end and not at the beginning
	  */
	def apply[A]()(implicit ord: Ordering[A]): SomeBeforeNone[A] = new SomeBeforeNone[A]()
}

/**
  * An ordering used with Options, that places the None at the end instead of the beginning.
  * @author Mikko Hilpinen
  * @since 8.12.2023, v2.3
  */
class SomeBeforeNone[A](implicit ord: Ordering[A]) extends Ordering[Option[A]]
{
	override def compare(x: Option[A], y: Option[A]): Int = x match {
		case Some(v1) =>
			y match {
				case Some(v2) => ord.compare(v1, v2)
				case None => -1
			}
		case None => if (y.isDefined) 1 else 0
	}
}
