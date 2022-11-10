package utopia.paradigm.enumeration

import utopia.flow.operator.{RichComparable, Sign}
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.paradigm.shape.shape1d.Vector1D

/**
  * An axis specifies the plane or the binary direction on which a length applies,
  * such as X (horizontal) or Y (vertical).
  * @author Mikko Hilpinen
  * @since Genesis 20.11.2018
  **/
sealed trait Axis extends RichComparable[Axis]
{
    // ABSTRACT -----------------------
    
    /**
      * @return The index of this axis in a context where dimensions are stored within an array or a sequence
      */
    def index: Int
    
    
    // COMPUTED ------------------------
    
    /**
      * @return A unit vector along this axis
      */
    def unit = Vector1D.unitAlong(this)
    
    
    // IMPLEMENTED  --------------------
    
    override def compareTo(o: Axis) = index - o.index
    
    
    // OTHER    ------------------------
    
    /**
      * A vector along this axis with the specified length
      */
    def apply(length: Double) = Vector1D(length, this)
}

/**
  * This trait is common for axes on the 2D-plane
  */
sealed trait Axis2D extends Axis
{
    // ABSTRACT ----------------------------
    
    /**
      * The axis perpendicular to this one
      */
    def perpendicular: Axis2D
    
    /**
      * @param alignment A linear alignment
      * @return That linear alignment when applied over this axis
      */
    def apply(alignment: LinearAlignment): Alignment
    
    
    // COMPUTED ----------------------------
    
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

object Axis2D
{
    /**
     * All possible values of this trait
     */
    val values: Vector[Axis2D] = Vector(Axis.X, Axis.Y)
}

object Axis
{
    // ATTRIBUTES   ------------------
    
    /**
     * All possible values of this trait
     */
    val values = Vector[Axis](X, Y, Z)
    
    
    // COMPUTED ---------------------
    
    /**
      * @return The X-axis
      */
    def horizontal = X
    /**
      * @return The Y-axis
      */
    def vertical = Y
    
    
    // OTHER    ---------------------
    
    /**
      * @param index An axis index (0-based)
      * @return The axis matching that index
      */
    def apply(index: Int) = values(index)
    
    
    // VALUES   ---------------------
    
    /**
      * The X-axis typically represents object width / horizontal coordinates
      */
    case object X extends Axis2D
    {
        override def index = 0
        def perpendicular = Y
        def apply(alignment: LinearAlignment): Alignment = Alignment.horizontal(alignment)
    }
    
    /**
      * The Y-axis typically represents object height / vertical coordinates
      */
    case object Y extends Axis2D
    {
        override def index = 1
        def perpendicular = X
        def apply(alignment: LinearAlignment): Alignment = Alignment.vertical(alignment)
    }
    
    /**
      * The Z-axis is used in 3 dimensional shapes to represent depth
      */
    case object Z extends Axis
    {
        override def index = 2
    }
}