package utopia.paradigm.shape.template

import utopia.flow.operator.EqualsFunction
import utopia.flow.util.CollectionExtensions._
import utopia.paradigm.enumeration.{Axis, Axis2D}
import utopia.paradigm.enumeration.Axis.{X, Y, Z}

/**
  * Trait for items which can be split along different dimensions (which are represented by axes)
  * @author Mikko Hilpinen
  * @since Genesis 13.9.2019, v2.1+
  */
trait Dimensional[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @return The X, Y, Z ... dimensions of this vector-like instance. No specific length required, however.
	  */
	def dimensions: Seq[A]
	
	/**
	  * @return A value with length of zero
	  */
	protected def zeroDimension: A
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return A map with axes as keys and dimensions as values. Only supports up to 3 axes, all of which
	  *         might not be present in the resulting map.
	  */
	def toMap = dimensions.take(3).zipWithIndex.map { case (v, i) =>
		axisForIndex(i) -> v }.toMap
	
	
	// IMPLEMENTED  ----------------
	
	override def toString = s"(${dimensions.mkString(", ")})"
	
	
	// OTHER	--------------------
	
	/**
	  * @param axis Target axis
	  * @return This instance's component along specified axis
	  */
	def along(axis: Axis) = dimensions.getOrElse(indexForAxis(axis), zeroDimension)
	
	/**
	  * @param axis Target axis
	  * @return This instance's component perpendicular to targeted axis
	  */
	def perpendicularTo(axis: Axis2D) = along(axis.perpendicular)
	
	/**
	  * @param axis Target axis
	  * @return Index in the dimensions array for the specified axis
	  */
	def indexForAxis(axis: Axis) = axis match
	{
		case X => 0
		case Y => 1
		case Z => 2
	}
	/**
	  * @param index An index
	  * @return An axis that matches that index
	  * @throws IndexOutOfBoundsException If passing an index larger than 2 or smaller than 0
	  */
	@throws[IndexOutOfBoundsException]("Only indices 0, 1, and 2 are allowed")
	protected def axisForIndex(index: Int) = index match
	{
		case 0 => X
		case 1 => Y
		case 2 => Z
		case _ => throw new IndexOutOfBoundsException(s"No axis for index $index")
	}
	
	/**
	  * Checks whether these two dimensional items are equal when using the specified equality function
	  * @param other Another dimensional item
	  * @param f Equality function
	  * @tparam B Type of the other dimensions
	  * @return Whether these two items are equal, according to the equality function
	  */
	def compareEqualityWith[B](other: Dimensional[B])(f: (A, B) => Boolean) =
	{
		val myDimensions = dimensions
		val otherDimensions = other.dimensions
		(0 until (myDimensions.size max otherDimensions.size)).forall { index =>
			f(myDimensions.getOrElse(index, zeroDimension), otherDimensions.getOrElse(index, other.zeroDimension))
		}
	}
	
	/**
	  * @param other Another dimensional item
	  * @param equals A custom equals function (implicit)
	  * @tparam B Type of the lengths in the other dimensional item
	  * @return Whether these two items are equal when using the specified equals function
	  */
	def ~==[B >: A](other: Dimensional[B])(implicit equals: EqualsFunction[B]) =
		compareEqualityWith(other) { equals(_, _) }
	def !~==[B >: A](other: Dimensional[B])(implicit equals: EqualsFunction[B]) = !(this ~== other)
}
