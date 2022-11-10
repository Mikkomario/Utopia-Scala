package utopia.paradigm.shape.template

import utopia.paradigm.enumeration.Axis

/**
  * A common trait for factory implementations that build items by wrapping a set of dimensions
  * @author Mikko Hilpinen
  * @since 9.11.2022, v1.2
  */
trait DimensionsWrapperFactory[D, +To] extends FromDimensionsFactory[D, To] with DimensionalFactory[D, To]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return A zero-value dimension
	  */
	def zeroDimension: D
	
	override def apply(dimensions: Dimensions[D]): To
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Factory used for building new dimensions to be wrapped
	  */
	protected def dimensionsFactory = Dimensions(zeroDimension)
	
	
	// IMPLEMENTED  --------------------------
	
	override def newBuilder = new DimensionsBuilder[D](zeroDimension).mapResult(apply)
	
	override def apply(values: IndexedSeq[D]): To = values match {
		case d: Dimensions[D] => apply(d)
		case o => apply(dimensionsFactory(o))
	}
	
	override def apply(values: Map[Axis, D]) = apply(dimensionsFactory(values))
	override def from(values: IterableOnce[D]) = apply(dimensionsFactory.from(values))
}
