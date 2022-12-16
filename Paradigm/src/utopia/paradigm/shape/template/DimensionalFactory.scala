package utopia.paradigm.shape.template

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis, Axis2D}
import utopia.paradigm.shape.template.DimensionalFactory.MappedDimensionalFactory

import scala.collection.BuildFrom
import scala.language.implicitConversions

object DimensionalFactory
{
	// IMPLICIT -------------------------
	
	implicit def factoryToBuildFrom[D, R](f: DimensionalFactory[D, R]): BuildFrom[Any, D, R] =
		new BuildDimensionalFrom[D, R](f)
	
	
	// NESTED   -------------------------
	
	private class MappedDimensionalFactory[D, Mid, R](o: DimensionalFactory[D, Mid])(f: Mid => R)
		extends DimensionalFactory[D, R]
	{
		override def newBuilder = o.newBuilder.mapResult(f)
		
		override def apply(values: IndexedSeq[D]) = f(o(values))
		override def apply(values: Map[Axis, D]) = f(o(values))
		override def from(values: IterableOnce[D]) = f(o.from(values))
	}
	
	private class BuildDimensionalFrom[-A, +To](f: DimensionalFactory[A, To]) extends BuildFrom[Any, A, To]
	{
		override def newBuilder(from: Any) = f.newBuilder
		
		override def fromSpecific(from: Any)(it: IterableOnce[A]) = f.from(it)
	}
}

/**
  * Common trait for factories that are used for building new dimensional items
  * @author Mikko Hilpinen
  * @since 6.11.2022, v1.2
  */
trait DimensionalFactory[-D, +R]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return A new builder for building dimension sets
	  */
	def newBuilder: DimensionalBuilder[D, R]
	
	/**
	  * @param values Dimensions to assign (ordered)
	  * @return A set of dimensions based on the specified values
	  */
	def apply(values: IndexedSeq[D]): R
	/**
	  * @param values Dimensions to assign (axis -> dimension)
	  * @return A set of dimensions based on the specified values
	  */
	def apply(values: Map[Axis, D]): R
	
	/**
	  * @param values Dimensions to assign (ordered)
	  * @return A set of dimensions based on the specified values
	  */
	def from(values: IterableOnce[D]): R
	
	
	// COMPUTED ---------------------------
	
	/**
	  * An empty dimensional item
	  */
	def empty = apply(Vector())
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param values Dimensions to assign (ordered)
	  * @return A set of dimensions based on the specified values
	  */
	def apply(values: D*): R = apply(values.toVector)
	
	/**
	  * @param parallel Dimension parallel to the specified axis
	  * @param perpendicular Dimension perpendicular to the specified axis
	  * @param along An axis
	  * @return An item with X and Y dimensions based on the specified values
	  */
	def apply(parallel: D, perpendicular: D, along: Axis2D): R = along match {
		case X => apply(parallel, perpendicular)
		case Y => apply(perpendicular, parallel)
	}
	
	/**
	  * @param length Target dimensions count
	  * @param elem An element to fill the dimensions up to 'length' (call-by-name)
	  * @return A new item with 'length' many 'elem' dimensions
	  */
	def fill(length: Int)(elem: => D) = apply(Vector.fill(length)(elem))
	/**
	  * @param length Target dimensions count [0, 3]
	  * @param f A function that accepts an axis and returns a dimension for that axis
	  * @return A new set of dimension based on the specified function's values
	  */
	def iterate(length: Int)(f: Axis => D) = apply(Axis.values.take(length).map(f))
	/**
	  * @param f A function that accepts an axis (X, Y) and produces a dimension
	  * @return A dimensional item consisting of the function return values (2)
	  */
	def fromFunction2D(f: Axis2D => D) = apply(Axis2D.values.map(f))
	/**
	  * @param f A function that accepts an axis (X, Y, Z) and produces a dimension
	  * @return A dimensional item consisting of the function return values (3)
	  */
	def fromFunction3D(f: Axis => D) = apply(Axis.values.map(f))
	
	/**
	  * Maps all items built with this factory
	  * @param f A mapping function to apply to all items
	  * @tparam R2 New result type
	  * @return A new factory that maps the results of this factory
	  */
	def mapResult[R2](f: R => R2): DimensionalFactory[D, R2] = new MappedDimensionalFactory[D, R, R2](this)(f)
}
