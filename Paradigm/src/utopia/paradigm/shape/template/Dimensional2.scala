package utopia.paradigm.shape.template

import utopia.flow.operator.CanBeZero
import utopia.paradigm.enumeration.Axis
import Axis._

object Dimensional2
{
	// type N[X] = DimensionsWrapper[X, DimensionsWrapper[X, _]]
}

/**
  * A common trait for items which have a set of dimensions (along X, Y, Z, ...),
  * and which may be copied with a different set of dimensions.
  * @author Mikko Hilpinen
  * @since 6.11.2022, v1.2
  */
trait Dimensional2[A, +Repr] extends HasDimensions[A] with CanBeZero[Repr]
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
	def mapEachDimension(f: A => A) = withDimensions(dimensions.map(f))
	def mapDimension(axis: Axis)(f: A => A) = mapDimensions { _.mapDimension(axis)(f) }
	def mapX(f: A => A) = mapDimension(X)(f)
	def mapY(f: A => A) = mapDimension(Y)(f)
	def mapZ(f: A => A) = mapDimension(Z)(f)
	
	def withDimension(axis: Axis, dimension: A) = mapDimensions { _.withDimension(axis, dimension) }
	def withX(x: A) = withDimension(X, x)
	def withY(y: A) = withDimension(Y, y)
	def withZ(z: A) = withDimension(Z, z)
}
