package utopia.paradigm.shape.template

import utopia.flow.operator.CanBeZero
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis._

/**
  * A common trait for items that can wrap a set of dimensions of any type
  * @author Mikko Hilpinen
  * @since 6.11.2022, v1.2
  * @tparam A Type of dimension values in this instance
  * @tparam Repr Type of this wrapper. Accepts a type of dimension.
  */
trait DimensionsWrapper[+A, +Repr[+X]] extends HasDimensions[A] with CanBeZero[Repr[A]]
{
	// ABSTRACT -------------------------
	
	/**
	  * Creates a copy of this item with a new set of dimensions
	  * @param newDimensions A new set of dimensions
	  * @tparam B Type of new dimensions to assign
	  * @return A copy of this item with those dimensions
	  */
	def withDimensions[B](newDimensions: Dimensions[B]): Repr[B]
	
	
	// IMPLEMENTED  ---------------------
	
	override def zero: Repr[A] = withDimensions(dimensions.zero)
	override def isZero = dimensions.isZero
	
	
	// OTHER    -------------------------
	
	/**
	  * @param f A mapping function used for modifying the wrapped set of dimensions
	  * @tparam B Type of new dimension values
	  * @return A copy of this item with a modified set of dimensions
	  */
	def mapDimensions[B](f: Dimensions[A] => Dimensions[B]) = withDimensions(f(dimensions))
	/**
	  * @param f A mapping function for modifying individual dimensions.
	  *          Will be called for each dimension of this item.
	  * @tparam B Type of new dimensions.
	  * @return A copy of this item with mapped dimensions
	  */
	def mapEachDimension[B](f: A => B) = withDimensions(dimensions.map(f))
	/**
	  * Modifies a single dimension of this item
	  * @param axis Targeted axis
	  * @param f A mapping function called for a dimension matching that axis
	  * @tparam B Type of mapping result
	  * @return A copy of this item with a single dimension mapped
	  */
	def mapDimension[B >: A](axis: Axis)(f: A => B): Repr[B] = mapDimensions[B] { _.mapDimension[B](axis)(f) }
	/**
	  * Modifies the x-dimension of this item
	  * @param f A mapping function called for the x-dimension
	  * @tparam B Type of mapping result
	  * @return A copy of this item with the mapped dimension
	  */
	def mapX[B >: A](f: A => B) = mapDimension[B](X)(f)
	/**
	  * Modifies the y-dimension of this item
	  * @param f A mapping function called for the y-dimension
	  * @tparam B Type of mapping result
	  * @return A copy of this item with the mapped dimension
	  */
	def mapY[B >: A](f: A => B) = mapDimension[B](Y)(f)
	/**
	  * Modifies the z-dimension of this item
	  * @param f A mapping function called for the z-dimension
	  * @tparam B Type of mapping result
	  * @return A copy of this item with the mapped dimension
	  */
	def mapZ[B >: A](f: A => B) = mapDimension[B](Z)(f)
	
	/**
	  * Replaces one of this item's dimensions
	  * @param axis Targeted axis
	  * @param dimension A new dimension to assign
	  * @tparam B Type of new dimension
	  * @return A copy of this item with that dimension
	  */
	def withDimension[B >: A](axis: Axis, dimension: B): Repr[B] = mapDimensions { _.withDimension(axis, dimension) }
	/**
	  * Replaces this item's x-dimension
	  * @param x The new x-dimension to assign
	  * @tparam B Type of new dimension
	  * @return A copy of this item with that dimension
	  */
	def withX[B >: A](x: B) = withDimension(X, x)
	/**
	  * Replaces this item's y-dimension
	  * @param y The new y-dimension to assign
	  * @tparam B Type of new dimension
	  * @return A copy of this item with that dimension
	  */
	def withY[B >: A](y: B) = withDimension(Y, y)
	/**
	  * Replaces this item's z-dimension
	  * @param z The new z-dimension to assign
	  * @tparam B Type of new dimension
	  * @return A copy of this item with that dimension
	  */
	def withZ[B >: A](z: B) = withDimension(Z, z)
}
