package utopia.paradigm.shape.template

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.EqualsFunction
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y, Z}

object HasDimensions
{
	/**
	  * Type alias for items which have dimensions of type Double
	  */
	type HasDoubleDimensions = HasDimensions[Double]
}

/**
  * Common trait for items which have a set of dimensions (X, Y, Z, ...).
  * Typically these are vectors or other elements which represent length or location in 1D, 2D or 3D space.
  * @author Mikko Hilpinen
  * @since 6.11.2022, v1.2
  */
trait HasDimensions[+A]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Dimensions of this item
	  */
	def dimensions: Dimensions[A]
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return The x-component of these dimensions
	  */
	def x = dimensions(X)
	/**
	  * @return The y-component of these dimensions
	  */
	def y = dimensions(Y)
	/**
	  * @return The z-component of these dimensions
	  */
	def z = dimensions(Z)
	
	/**
	  * @return The x and y dimensions as a pair
	  */
	def xyPair = Pair(x, y)
	
	/**
	  * @param ord Implicit ordering to use
	  * @tparam B Ordered type
	  * @return Smallest individual dimension in this item
	  */
	def minDimension[B >: A](implicit ord: Ordering[B]) = (dimensions :+ dimensions.zeroValue).min(ord)
	/**
	  * @param ord Implicit ordering to use
	  * @tparam B Ordered type
	  * @return Largest individual dimension in this item
	  */
	def maxDimension[B >: A](implicit ord: Ordering[B]) = (dimensions :+ dimensions.zeroValue).max(ord)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param axis Targeted axis
	  * @return This item's dimension along that axis
	  */
	def along(axis: Axis) = dimensions(axis)
	
	/**
	  * @param other Another item with dimensions
	  * @param equals Equality function for comparing dimensions
	  * @tparam B Type of dimensions in the other item
	  * @return Whether these two items have equal dimensions, when using the specified equality function
	  */
	def ~==[B >: A](other: HasDimensions[B])(implicit equals: EqualsFunction[B]): Boolean =
		dimensions ~== other.dimensions
	/**
	  * @param other Another item with dimensions
	  * @param equals Equality function for comparing dimensions
	  * @tparam B Type of dimensions in the other item
	  * @return Whether these two items have unequal dimensions, when using the specified equality function
	  */
	def !~==[B >: A](other: HasDimensions[B])(implicit equals: EqualsFunction[B]) = !(this ~== other)
	
	/**
	  * @param axis Target axis
	  * @return Whether this item has a positive (> 0) value for specified dimension
	  */
	def isPositiveAlong[B >: A](axis: Axis)(implicit ord: Ordering[B]) = ord.gteq(along(axis), dimensions.zeroValue)
	/**
	  * @param axis Target axis
	  * @return Whether this item has a negative (< 0) value for specified dimension
	  */
	def isNegativeAlong[B >: A](axis: Axis)(implicit ord: Ordering[B]) = ord.lteq(along(axis), dimensions.zeroValue)
	/**
	  * @param axis Targeted axis
	  * @return Whether this item contains a zero dimension / value for that axis
	  */
	def isZeroAlong(axis: Axis) = dimensions(axis) == dimensions.zeroValue
	
	/**
	  * Compares this item with another using 'forall'.
	  * Works even when these two items have a different number of dimensions.
	  * @param other Another dimensional item
	  * @param f A function that accepts a dimension (of same direction) from both of these items
	  *          and yields true or false
	  * @tparam B Type of dimensions in the other item
	  * @return Whether the specified function returned true for all dimensions between this and the other item
	  */
	def forAllDimensionsWith[B](other: HasDimensions[B])(f: (A, B) => Boolean) =
		dimensions.zipIteratorWith(other.dimensions).forall { case (my, their) => f(my, their) }
	/**
	  * Compares this item with another using 'exists'.
	  * Works even when these two items have a different number of dimensions.
	  * @param other Another dimensional item
	  * @param f A function that accepts a dimension (of same direction) from both of these items
	  *          and yields true or false
	  * @tparam B Type of dimensions in the other item
	  * @return Whether the specified function returned true for any dimensions between this and the other item
	  */
	def existsDimensionWith[B](other: HasDimensions[B])(f: (A, B) => Boolean) =
		dimensions.zipIteratorWith(other.dimensions).exists { case (my, their) => f(my, their) }
}
