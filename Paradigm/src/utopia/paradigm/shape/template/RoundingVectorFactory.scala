package utopia.paradigm.shape.template

import utopia.paradigm.shape.shape1d.rounding.RoundingDouble

/**
  * Common trait for factory classes used for building rounding vectors (see [[RoundingVectorLike]]).
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  * @tparam V Type of vectors constructed by this factory
  */
trait RoundingVectorFactory[+V] extends NumericVectorFactory[RoundingDouble, V]
{
	// COMPUTED --------------------------
	
	/**
	  * @return Copy of this factory that accepts non-rounded double numbers
	  */
	def forDoubles = FromDoubleFactory
	
	
	// IMPLEMENTED  ----------------------
	
	override implicit def n: Numeric[RoundingDouble] = RoundingDouble.numeric
	
	override def dimensionFrom(double: Double): RoundingDouble = RoundingDouble(double)
	
	override def scale(d: RoundingDouble, mod: Double): RoundingDouble = d * mod
	override def div(d: RoundingDouble, div: Double): RoundingDouble = if (div == 0.0) d else d / div
	
	
	// NESTED   --------------------------
	
	object FromDoubleFactory extends FromDimensionsFactory[Double, V]
	{
		override def apply(dimensions: Dimensions[Double]): V =
			RoundingVectorFactory.this.apply(dimensions.mapWithZero(RoundingDouble.zero) { RoundingDouble(_) })
		
		override def from(other: HasDimensions[Double]): V = apply(other.dimensions)
	}
}
