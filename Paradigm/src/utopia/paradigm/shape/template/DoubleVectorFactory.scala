package utopia.paradigm.shape.template

import scala.math.Numeric.DoubleIsFractional

/**
  * A common trait for factories used for building (double) vectors
  * @author Mikko Hilpinen
  * @since 9.11.2022, v1.2
  */
trait DoubleVectorFactory[+V] extends NumericVectorFactory[Double, V]
{
	// IMPLEMENTED  ---------------------
	
	override implicit def n: Numeric[Double] = DoubleIsFractional
	override protected def dimensionsFactory = Dimensions.double
	
	override def dimensionFrom(double: Double): Double = double
	override def scale(d: Double, mod: Double): Double = d * mod
	// Will never divide by zero
	override def div(d: Double, div: Double): Double = if (div == 0.0) d else div
	
	override def fromDoubles(v: HasDimensions[Double]) = from(v)
}
