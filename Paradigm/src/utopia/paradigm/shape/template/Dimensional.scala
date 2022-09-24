package utopia.paradigm.shape.template

import utopia.flow.operator.EqualsFunction
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.UncertainBoolean.{Certain, Undefined}
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
	def dimensions: IndexedSeq[A]
	
	/**
	  * @return A value with length of zero
	  */
	def zeroDimension: A
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return A map with axes as keys and dimensions as values. Only supports up to 3 axes, all of which
	  *         might not be present in the resulting map.
	  */
	def toMap = dimensions.take(3).zipWithIndex.map { case (v, i) => Axis(i) -> v }.toMap
	
	
	// IMPLEMENTED  ----------------
	
	override def toString = s"(${dimensions.mkString(", ")})"
	
	
	// OTHER	--------------------
	
	/**
	  * @param axis Target axis
	  * @return This instance's component along specified axis
	  */
	def along(axis: Axis) = dimensions.getOrElse(axis.index, zeroDimension)
	
	/**
	  * @param axis Target axis
	  * @return This instance's component perpendicular to targeted axis
	  */
	def perpendicularTo(axis: Axis2D) = along(axis.perpendicular)
	
	/**
	  * @param axis Target axis
	  * @return Index in the dimensions array for the specified axis
	  */
	@deprecated("Please use axis.index instead", "v1.1")
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
	@deprecated("Please use Axis(Int) instead", "v1.1")
	protected def axisForIndex(index: Int): Axis = index match {
		case 0 => X
		case 1 => Y
		case 2 => Z
		case _ => throw new IndexOutOfBoundsException(s"No axis for index $index")
	}
	
	/**
	  * @param other Another dimensional item
	  * @tparam B Type of dimensions in the other item
	  * @return Dimensions of both of these items zipped,
	  *         so that the length of the resulting Seq is equal to the maximum number of dimensions between these items
	  */
	def zipDimensionsWith[B](other: Dimensional[B]) =
		dimensions.zipPad(other.dimensions, zeroDimension, other.zeroDimension)
	/**
	  * @param other Another dimensional item
	  * @tparam B Type of dimensions in the other item
	  * @return An iterator that returns zipped dimensions from both of these items,
	  *         so that the length of the resulting iterator is equal to the
	  *         maximum number of dimensions between these items
	  */
	def zipDimensionsIteratorWith[B](other: Dimensional[B]) =
		dimensions.iterator.zipPad(other.dimensions.iterator, zeroDimension, other.zeroDimension)
	
	/**
	  * Compares this item with another using 'forall'.
	  * Works even when these two items have a different number of dimensions.
	  * @param other Another dimensional item
	  * @param f A function that accepts a dimension (of same direction) from both of these items
	  *          and yields true or false
	  * @tparam B Type of dimensions in the other item
	  * @return Whether the specified function returned true for all dimensions between this and the other item
	  */
	def forAllDimensionsWith[B](other: Dimensional[B])(f: (A, B) => Boolean) =
		zipDimensionsIteratorWith(other).forall { case (my, their) => f(my, their) }
	/**
	  * Compares this item with another using 'exists'.
	  * Works even when these two items have a different number of dimensions.
	  * @param other Another dimensional item
	  * @param f A function that accepts a dimension (of same direction) from both of these items
	  *          and yields true or false
	  * @tparam B Type of dimensions in the other item
	  * @return Whether the specified function returned true for any dimensions between this and the other item
	  */
	def existsDimensionWith[B](other: Dimensional[B])(f: (A, B) => Boolean) =
		zipDimensionsIteratorWith(other).exists { case (my, their) => f(my, their) }
	/**
	  * Checks whether these two dimensional items are equal when using the specified equality function
	  * @param other Another dimensional item
	  * @param f Equality function
	  * @tparam B Type of the other dimensions
	  * @return Whether these two items are equal, according to the equality function
	  */
	@deprecated("Please use testEqualityWith(...) instead", "v1.1")
	def compareEqualityWith[B](other: Dimensional[B])(f: (A, B) => Boolean) = testEqualityWith(other)(f)
	/**
	  * Tests whether these two items may be considered equal using the specified equality function.
	  * This function works even when these two items have a different number of dimensions.
	  * @param other Another dimensional item
	  * @param equals A function that accepts a dimension from this item and a dimension from the other item and
	  *               returns whether the two dimensions may be considered equal
	  * @tparam B Type of dimensions in the other item
	  * @return Whether these two items may be considered equal
	  */
	def testEqualityWith[B](other: Dimensional[B])(equals: (A, B) => Boolean) =
		forAllDimensionsWith(other)(equals)
	/**
	  * Compares the dimensions of this item to the dimensions of another.
	  * This function works even when these two items have different number of dimensions.
	  * @param other Another dimensional item
	  * @param f A function that compares two dimensions of the same direction and yields either true or false
	  * @tparam B Type of dimensions in the other item
	  * @return True if 'f' yielded true for all dimensions,
	  *         False if 'f' yielded false for all dimensions,
	  *         Undefined otherwise
	  */
	def compareDimensions[B](other: Dimensional[B])(f: (A, B) => Boolean) =
	{
		val zippedDimensions = zipDimensionsWith(other)
		val sidesCount = zippedDimensions.count { case (my, their) => f(my, their) }
		
		// Case: f yielded false for all items => False
		if (sidesCount == 0)
			Certain(false)
		// Case: f yielded false for some and true for some => Undefined
		else if (sidesCount < zippedDimensions.size)
			Undefined
		// Case: f yielded true for all items => True
		else
			Certain(true)
	}
	
	/**
	  * @param other Another dimensional item
	  * @param equals A custom equals function (implicit)
	  * @tparam B Type of the lengths in the other dimensional item
	  * @return Whether these two items are equal when using the specified equals function
	  */
	def ~==[B >: A](other: Dimensional[B])(implicit equals: EqualsFunction[B]) =
		testEqualityWith(other)(equals.apply)
	def !~==[B >: A](other: Dimensional[B])(implicit equals: EqualsFunction[B]) = !(this ~== other)
}
