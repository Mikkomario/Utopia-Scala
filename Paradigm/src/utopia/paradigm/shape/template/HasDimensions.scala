package utopia.paradigm.shape.template

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.EqualsFunction
import utopia.paradigm.enumeration.Axis.{X, Y, Z}

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
	
	
	// OTHER    ------------------------
	
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
}
