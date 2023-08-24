package utopia.paradigm.shape.shape2d

import utopia.flow.operator.{EqualsExtensions, EqualsFunction, HasLength}
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.{Dimensions, DimensionsWrapperFactory, DoubleVectorLike}

/**
  * Common trait for double-based vectors that use at least 2 dimensions
  * (and are suitable results for vector projections, etc.)
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
trait DoubleVectorLike2D[+Repr <: HasDoubleDimensions with HasLength] extends DoubleVectorLike[Repr, Repr]
{
	override protected def fromDoublesFactory: DimensionsWrapperFactory[Double, Repr] = factory
	
	override def scaled(xScaling: Double, yScaling: Double) = this * Dimensions.double(xScaling, yScaling)
	override def scaled(modifier: Double) = this * modifier
	override def translated(translation: HasDoubleDimensions) = this + translation
	
	// Slightly optimized overrides
	override def transformedWith(transformation: Matrix2D) =
		factory.from(transformation(dimensions.padTo(2, 1.0)))
	override def transformedWith(transformation: Matrix3D) =
		factory.from(transformation(dimensions.padTo(3, 1.0)))
}
