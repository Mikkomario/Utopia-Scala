package utopia.paradigm.shape.template.vector

import utopia.flow.operator.HasLength
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.util.Mutate
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

/**
  * This is a common trait for Distance-based 1D, 2D, and 3D vector implementations
  * @tparam Repr Concrete implementation of this trait
  * @author Mikko Hilpinen
  * @since 01.01.2025, v1.7.1
  */
trait DistanceVectorLike[+Repr <: HasDimensions[Distance] with HasLength]
	extends NumericVectorLike[Distance, Repr, Repr]
{
	// IMPLEMENTED  ----------------------
	
	override implicit def dimensionApproxEquals: EqualsFunction[Distance] = Distance.approxEquals
	
	override def toDoublePrecision: Repr = self
	
	override def mapToDouble(f: Distance => Double) = map { d => d.copy(amount = f(d)) }
	
	override def scaledBy(mod: Double) = mapLengths { _ * mod }
	override def scaledBy(other: HasDoubleDimensions) =
		withDimensions(dimensions.mergeWith(other, dimensions.zeroValue) { _ * _ })
	override def dividedBy(div: Double) = mapLengths { _ / div }
	override def dividedBy(other: HasDoubleDimensions) =
		withDimensions(dimensions.mergeWith[Double, Distance](other, dimensions.zeroValue) {
			(len, div) => if (div == 0) len else len / div })
	
	override def scaled(xScaling: Double, yScaling: Double) = this * Dimensions.double(xScaling, yScaling)
	override def scaled(modifier: Double) = this * modifier
	override def translated(translation: HasDoubleDimensions) = this + fromDoublesFactory.from(translation)
	
	
	// OTHER    --------------------------
	
	def *(mod: Double) = scaledBy(mod)
	def /(div: Double) = dividedBy(div)
	
	/**
	  * Transforms the lengths of this vector's components, without affecting / regardless of the unit of measurement
	  * @param f A mapping function targeting raw length amounts regardless of unit
	  * @return Mapped copy of this vector
	  */
	def mapLengths(f: Mutate[Double]) = map { d => d.copy(amount = f(d.amount)) }
}
