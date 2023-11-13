package utopia.paradigm.shape.shape2d.vector.point

import utopia.flow.operator.equality.EqualsBy
import utopia.paradigm.shape.shape1d.RoundingDouble
import utopia.paradigm.shape.template.vector.{RoundingVector, RoundingVectorFactory, RoundingVectorLike}
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

object RoundingPoint extends RoundingVectorFactory[RoundingPoint]
{
	// IMPLEMENTED  -------------------------
	
	override def apply(dimensions: Dimensions[RoundingDouble]): RoundingPoint = new RoundingPoint(dimensions.in2D)
	
	override def from(other: HasDimensions[RoundingDouble]): RoundingPoint = other match {
		case p: RoundingPoint => p
		case o => apply(o.dimensions)
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param p A java AWT point
	  * @return A rounding point containing the same information
	  */
	def apply(p: java.awt.Point): RoundingPoint = apply(RoundingDouble(p.getX), RoundingDouble(p.getY))
	/**
	  * @param p A java AWT 2D point
	  * @return A rounding point containing the same information
	  */
	def apply(p: java.awt.geom.Point2D): RoundingPoint = apply(RoundingDouble(p.getX), RoundingDouble(p.getY))
}

/**
  * A point in two-dimensional space that utilizes rounding
  * @author Mikko Hilpinen
  * @since 25.8.2023, v1.4
  */
class RoundingPoint private(override val dimensions: Dimensions[RoundingDouble])
	extends PointLike[RoundingDouble, RoundingPoint] with RoundingVectorLike[RoundingPoint] with RoundingVector
		with EqualsBy
{
	override def self: RoundingPoint = this
	override protected def factory: RoundingVectorFactory[RoundingPoint] = RoundingPoint
	override protected def equalsProperties: Iterable[Any] = dimensions
}
