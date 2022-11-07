package utopia.paradigm.shape.template

import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.template.DimensionalFactory.MappedDimensionalFactory

object DimensionalFactory
{
	private class MappedDimensionalFactory[D, Mid, R](o: DimensionalFactory[D, Mid])(f: Mid => R)
		extends DimensionalFactory[D, R]
	{
		override def newBuilder = o.newBuilder.mapResult(f)
		
		override def apply(values: IndexedSeq[D]) = f(o(values))
		override def apply(values: Map[Axis, D]) = f(o(values))
		override def from(values: IterableOnce[D]) = f(o.from(values))
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
	def apply(values: D*): R = apply(values.toIndexedSeq)
	
	/**
	  * Maps all items built with this factory
	  * @param f A mapping function to apply to all items
	  * @tparam R2 New result type
	  * @return A new factory that maps the results of this factory
	  */
	def mapResult[R2](f: R => R2): DimensionalFactory[D, R2] = new MappedDimensionalFactory[D, R, R2](this)(f)
}
