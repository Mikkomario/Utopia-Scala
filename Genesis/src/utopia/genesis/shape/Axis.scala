package utopia.genesis.shape

import utopia.genesis.shape.shape2D.Direction2D

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
        def toUnitVector = Vector3D(1)
        def perpendicular = Y
    }
    
    /**
      * The Y-axis typically represents object height / vertical coordinates
      */
    case object Y extends Axis2D
    {
        def toUnitVector = Vector3D(0, 1)
        def perpendicular = X
    }
    
    /**
      * The Z-axis is used in 3 dimensional shapes to represent depth
      */
    case object Z extends Axis
    {
        def toUnitVector = Vector3D(0, 0, 1)
    }
}

/**
* An axis represents a direction
* @author Mikko Hilpinen
* @since 20.11.2018
**/
sealed trait Axis
{
    /**
     * An unit vector along this axis
     */
    def toUnitVector: Vector3D
    
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
    // COMPUTED ----------------------------
    
    /**
     * The axis perpendicular to this one
     */
    def perpendicular: Axis2D
    
    /**
      * @return Directions associated with this axis
      */
    def directions = Direction2D.along(this)
    
    /**
      * @return Forward (positive) direction on this axis
      */
    def forward = direction(isPositive = true)
    
    /**
      * @return Backward (negative) direction on this axis
      */
    def backward = direction(isPositive = false)
    
    
    // OTHER    ----------------------------
    
    /**
      * @param isPositive Whether direction should be positive (true) or negative (false)
      * @return A direction along this axis with specified sign
      */
    def direction(isPositive: Boolean) = Direction2D(this, isPositive)
}