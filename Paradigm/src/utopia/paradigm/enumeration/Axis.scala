package utopia.paradigm.enumeration

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.paradigm.shape.shape2d.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.VectorLike

object Axis2D
{
    /**
     * All possible values of this trait
     */
    val values: Vector[Axis2D] = Vector(Axis.X, Axis.Y)
}

object Axis
{
    /**
     * All possible values of this trait
     */
    val values: Vector[Axis] = Axis2D.values :+ Z
    
    
    // VALUES   ---------------------
    
    /**
      * The X-axis typically represents object width / horizontal coordinates
      */
    case object X extends Axis2D
    {
        def toUnitVector = Vector2D(1)
        def perpendicular = Y
        def apply(alignment: LinearAlignment): Alignment = Alignment.horizontal(alignment)
    }
    
    /**
      * The Y-axis typically represents object height / vertical coordinates
      */
    case object Y extends Axis2D
    {
        def toUnitVector = Vector2D(0, 1)
        def perpendicular = X
        def apply(alignment: LinearAlignment): Alignment = Alignment.vertical(alignment)
    }
    
    /**
      * The Z-axis is used in 3 dimensional shapes to represent depth
      */
    case object Z extends Axis
    {
        def toUnitVector = Vector3D(0, 0, 1)
    
        override def toUnitVector3D = toUnitVector
    
        override def apply(length: Double) = Vector3D(0, 0, length)
    }
}

/**
* An axis represents a direction
* @author Mikko Hilpinen
* @since Genesis 20.11.2018
**/
sealed trait Axis
{
    /**
     * A unit vector along this axis
     */
    def toUnitVector: VectorLike[_ <: VectorLike[_]]
    
    /**
      * @return A 3D unit vector along this axis
      */
    def toUnitVector3D: Vector3D
    
    /**
     * A vector along this axis with the specified length
     */
    def apply(length: Double) = toUnitVector * length
}

/**
 * This trait is common for axes on the 2D-plane
 */
sealed trait Axis2D extends Axis
{
    // ABSTRACT ----------------------------
    
    override def toUnitVector: Vector2D
    
    override def apply(length: Double) = toUnitVector * length
    
    /**
      * @param alignment A linear alignment
      * @return That linear alignment when applied over this axis
      */
    def apply(alignment: LinearAlignment): Alignment
    
    
    // COMPUTED ----------------------------
    
    /**
     * The axis perpendicular to this one
     */
    def perpendicular: Axis2D
    
    override def toUnitVector3D = toUnitVector.in3D
    
    /**
      * @return Directions associated with this axis
      */
    def directions = Direction2D.along(this)
    
    /**
      * @return Forward (positive) direction on this axis
      */
    def forward = toDirection(Positive)
    /**
      * @return Backward (negative) direction on this axis
      */
    def backward = toDirection(Negative)
    
    
    // OTHER    ----------------------------
    
    /**
      * @param isPositive Whether direction should be positive (true) or negative (false)
      * @return A direction along this axis with specified sign
      */
    @deprecated("Please use toDirection(Sign) instead")
    def direction(isPositive: Boolean) = Direction2D(this, if (isPositive) Positive else Negative)
    
    /**
      * @param sign Sign of the resulting direction
      * @return A direction along this axis with specified sign
      */
    def toDirection(sign: Sign) = Direction2D(this, sign)
    /**
      * @param sign Sign of the resulting direction
      * @return A direction along this axis with the specified sign
      */
    def apply(sign: Sign) = toDirection(sign)
}