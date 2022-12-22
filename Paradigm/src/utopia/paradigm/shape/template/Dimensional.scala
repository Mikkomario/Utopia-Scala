package utopia.paradigm.shape.template

import utopia.flow.operator.{CanBeZero, Combinable, Reversible, Scalable}
import utopia.paradigm.enumeration.Axis
import Axis._

object Dimensional
{
	// EXTENSIONS   ----------------------------
	
	implicit class NumericDimensional[A, +R](val d: Dimensional[A, R])(implicit n: Numeric[A])
		extends Combinable[HasDimensions[A], R] with Reversible[R] with Scalable[A, R]
	{
		override def self = d.self
		
		override def unary_- = d.mapEachDimension(n.negate)
		
		override def +(other: HasDimensions[A]) = d.mergeWith(other)(n.plus)
		override def *(mod: A) = d.mapEachDimension { n.times(_, mod) }
	}
	
	implicit class CombiningDimensional[A <: Combinable[C, A], -C, +R](val d: Dimensional[A, R])
		extends AnyVal with Combinable[HasDimensions[C], R]
	{
		override def +(other: HasDimensions[C]) = d.mergeWith(other) { _ + _ }
	}
	
	implicit class ScalableDimensional[A <: Scalable[S, A], -S, +R](val d: Dimensional[A, R])
		extends AnyVal with Scalable[HasDimensions[S], R]
	{
		override def *(mod: HasDimensions[S]) = d.mergeWith(mod) { _ * _ }
		
		def *(mod: S) = d.mapEachDimension { _ * mod }
	}
	
	implicit class ReversibleDimensional[A <: Reversible[A], +R](val d: Dimensional[A, R])
		extends AnyVal with Reversible[R]
	{
		override def self = d.self
		
		override def unary_- = d.mapEachDimension { -_ }
	}
}

/**
  * A common trait for items which have a set of dimensions (along X, Y, Z, ...),
  * and which may be copied with a different set of dimensions.
  * @author Mikko Hilpinen
  * @since 6.11.2022, v1.2
  */
trait Dimensional[A, +Repr] extends HasDimensions[A] with CanBeZero[Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * Creates a copy of this item with a new set of dimensions
	  * @param newDimensions A new set of dimensions
	  * @return A copy of this item with those dimensions
	  */
	def withDimensions(newDimensions: Dimensions[A]): Repr
	
	
	// IMPLEMENTED  ---------------------
	
	override def zero = withDimensions(dimensions.zero)
	override def isZero = dimensions.isZero
	
	
	// OTHER    -------------------------
	
	/**
	  * @param f A function that accepts this item's dimensions and returns a modified copy
	  * @return A copy of this item with the mapped dimensions
	  */
	def mapDimensions(f: Dimensions[A] => Dimensions[A]) = withDimensions(f(dimensions))
	/**
	  * @param f A mapping function for modifying individual dimensions.
	  *          Will be called for each dimension of this item.
	  * @return A copy of this item with mapped dimensions
	  */
	def mapEachDimension(f: A => A) = withDimensions(dimensions.map(f))
	/**
	  * Modifies a single dimension of this item
	  * @param axis Targeted axis
	  * @param f A mapping function called for a dimension matching that axis
	  * @return A copy of this item with a single dimension mapped
	  */
	def mapDimension(axis: Axis)(f: A => A) = mapDimensions { _.mapDimension(axis)(f) }
	/**
	  * Modifies the x-dimension of this item
	  * @param f A mapping function called for the x-dimension
	  * @return A copy of this item with the mapped dimension
	  */
	def mapX(f: A => A) = mapDimension(X)(f)
	/**
	  * Modifies the y-dimension of this item
	  * @param f A mapping function called for the y-dimension
	  * @return A copy of this item with the mapped dimension
	  */
	def mapY(f: A => A) = mapDimension(Y)(f)
	/**
	  * Modifies the z-dimension of this item
	  * @param f A mapping function called for the z-dimension
	  * @return A copy of this item with the mapped dimension
	  */
	def mapZ(f: A => A) = mapDimension(Z)(f)
	
	/**
	  * Replaces one of this item's dimensions
	  * @param axis Targeted axis
	  * @param dimension A new dimension to assign
	  * @return A copy of this item with that dimension
	  */
	def withDimension(axis: Axis, dimension: A) = mapDimensions { _.withDimension(axis, dimension) }
	/**
	  * Replaces this item's x-dimension
	  * @param x The new x-dimension to assign
	  * @return A copy of this item with that dimension
	  */
	def withX(x: A) = withDimension(X, x)
	/**
	  * Replaces this item's y-dimension
	  * @param y The new y-dimension to assign
	  * @return A copy of this item with that dimension
	  */
	def withY(y: A) = withDimension(Y, y)
	/**
	  * Replaces this item's z-dimension
	  * @param z The new z-dimension to assign
	  * @return A copy of this item with that dimension
	  */
	def withZ(z: A) = withDimension(Z, z)
	
	/**
	  * @param other Another dimensional item
	  * @param f A merge function for individual dimensions
	  * @tparam B Type of dimensions in the other item
	  * @return A merged copy of these items
	  */
	def mergeWith[B](other: HasDimensions[B])(f: (A, B) => A) =
		withDimensions(dimensions.mergeWith(other.dimensions)(f))
	
	/**
	  * @param other Another dimensional item
	  * @param ord Ordering to use
	  * @return A combination of these two items where each dimension is the minimum between the two
	  */
	def topLeft(other: HasDimensions[A])(implicit ord: Ordering[A]) = mergeWith(other)(ord.min)
	/**
	  * @param other Another dimensional item
	  * @param ord Ordering to use
	  * @return A combination of these two items where each dimension is the maximum between the two
	  */
	def bottomRight(other: HasDimensions[A])(implicit ord: Ordering[A]) = mergeWith(other)(ord.max)
	
	/**
	  * @param axis Target axis
	  * @return This item if it has a >= 0 dimension on that axis.
	  *         Otherwise a copy of this item with a zero value on that axis.
	  */
	def positiveAlong(axis: Axis)(implicit ord: Ordering[A]) = {
		if (isNegativeAlong(axis)) withDimension(axis, dimensions.zeroValue) else self
	}
	/**
	  * @param axis Target axis
	  * @return This item if it has a <= 0 dimension on that axis.
	  *         Otherwise a copy of this item with a zero value on that axis.
	  */
	def negativeAlong(axis: Axis)(implicit ord: Ordering[A]) = {
		if (isPositiveAlong(axis)) withDimension(axis, dimensions.zeroValue) else self
	}
}
