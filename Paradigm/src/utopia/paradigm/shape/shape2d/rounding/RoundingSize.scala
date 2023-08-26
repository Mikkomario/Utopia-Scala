package utopia.paradigm.shape.shape2d.rounding

import utopia.flow.operator.{EqualsBy, SignedOrZero}
import utopia.paradigm.shape.shape1d.rounding.RoundingDouble
import utopia.paradigm.shape.shape2d.{SizeFactoryLike, SizeLike}
import utopia.paradigm.shape.template.{Dimensions, HasDimensions, RoundingVector, RoundingVectorFactory, RoundingVectorLike}
import utopia.paradigm.transform.SizeAdjustable

object RoundingSize extends SizeFactoryLike[RoundingDouble, RoundingSize] with RoundingVectorFactory[RoundingSize]
{
    // ATTRIBUTES   --------------------------
    
    override val zero = super.zero
    
    
    // IMPLEMENTED  --------------------------
    
    override def apply(dimensions: Dimensions[RoundingDouble]): RoundingSize = new RoundingSize(dimensions.in2D)
    
    override def from(other: HasDimensions[RoundingDouble]): RoundingSize = other match {
        case s: RoundingSize => s
        case o => apply(o.dimensions)
    }
}

/**
* Used for representing a two-dimensional size (width + height) with automatic rounding in place
* @author Mikko Hilpinen
* @since 26.8.2023, v1.4
**/
class RoundingSize private(override val dimensions: Dimensions[RoundingDouble])
    extends SizeLike[RoundingDouble, RoundingSize, RoundingSize]
        with RoundingVectorLike[RoundingSize] with RoundingVector
        with SignedOrZero[RoundingSize]
        with RoundingSized[RoundingSize] with SizeAdjustable[RoundingSize] with EqualsBy
{
    // IMPLEMENTED    -----------------------
    
    override implicit def n: Fractional[RoundingDouble] = RoundingDouble.numeric
    
    override def self = this
    override def size = this
    override protected def factory = RoundingSize
    
    override def width = x
    override def height = y
    
    override def zero = RoundingSize.zero
    
    override protected def equalsProperties = dimensions
}