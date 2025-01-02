package utopia.paradigm.shape.template.vector

import utopia.paradigm.measurement.Distance.DistanceIsFractionalIn
import utopia.paradigm.measurement.{Distance, DistanceUnit}
import utopia.paradigm.shape.template.{Dimensions, FromDimensionsFactory, HasDimensions}

/**
  * Common parent class for factories used for constructing distance vectors
  * @author Mikko Hilpinen
  * @since 01.01.2025, v1.7.1
  * @param unit Unit assumed when no other unit has been specified
  */
abstract class DistanceVectorFactory[+V](unit: DistanceUnit) extends NumericVectorFactory[Distance, V]
{
	// ATTRIBUTES   ----------------------
	
	override implicit val n: Fractional[Distance] = DistanceIsFractionalIn(unit)
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return An interface to this factory which accepts double numbers.
	  *         Assumes this factory's unit of measurement.
	  */
	def fromDoublesFactory: FromDimensionsFactory[Double, V] = FromDoublesFactory
	
	
	// IMPLEMENTED  ----------------------
	
	override def dimensionFrom(double: Double): Distance = Distance(double, unit)
	
	override def scale(d: Distance, mod: Double): Distance = d * mod
	override def div(d: Distance, div: Double): Distance = if (div == 0.0) d else d/div
	
	
	// NESTED   --------------------------
	
	private object FromDoublesFactory extends FromDimensionsFactory[Double, V]
	{
		override def apply(dimensions: Dimensions[Double]): V = DistanceVectorFactory.this(dimensions.map(dimensionFrom))
		override def from(other: HasDimensions[Double]): V = apply(other.dimensions)
	}
}
