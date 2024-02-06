package utopia.paradigm.angular

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.equality.{ApproxSelfEquals, EqualsFunction}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.ordering.SelfComparable
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.generic.ParadigmDataType.AngleType
import utopia.paradigm.transform.LinearSizeAdjustable

object Angle extends RotationFactory[Angle]
{
    // ATTRIBUTES    -----------------------------
    
    /**
      * A function that returns true when the two specified angles are approximately equal to each other
      */
    val approxEquals: EqualsFunction[Angle] = _.radians ~== _.radians
    
    /**
     * Angle that points to the left (180 degrees)
     */
    val left = radians(math.Pi)
    /**
     * Angle that points to the right (0 degrees)
     */
    val right = radians(0)
    /**
     * Angle that points up (270 degrees)
     */
    val up = radians(3 * math.Pi / 2)
    /**
     * Angle that points down (90 degrees)
     */
    val down = radians(math.Pi / 2)
    
    /**
      * The red color angle when using HSL
      */
    val red = radians(0)
    /**
      * The green color angle when using HSL
      */
    val green = radians(2 * math.Pi / 3)
    /**
      * The blue color angle when using HSL
      */
    val blue = radians(4 * math.Pi / 3)
    
    
    // COMPUTED ----------------------------------
    
    /**
      * @return 1/2 of a circle
      */
    def half = left
    /**
      * @return 3/4 of a circle
      */
    override def threeQuarters = up
    
    
    // IMPLEMENTED  ----------------------
    
    override def radians(rads: Double): Angle = {
        val raw = rads % (2 * math.Pi)
        if (raw < 0) apply(raw + 2 * math.Pi) else apply(raw)
    }
    
    
    // FACTORIES    ------------------------------
    
    /**
     * Converts a radian angle to an angle instance
     */
    @deprecated("Please use .radians(Double) instead", "v1.5")
    def ofRadians(radians: Double) = this.radians(radians)
    /**
     * Converts a degrees angle to an angle instance (some inaccuracy may occur since the value 
     * is converted to radians internally)
     */
    @deprecated("Please use .degrees(Double) instead", "v1.5")
    def ofDegrees(degrees: Double) = this.degrees(degrees)
    /**
     * @param circleRatio Ratio of a circle [0, 1] to the positive (clockwise) direction
     * @return That ratio as an angle
     */
    @deprecated("Please use .circles(Double) instead", "v1.5")
    def ofCircles(circleRatio: Double) = circles(circleRatio)
    
    
    // OTHER    ----------------------------------
    
    /**
     * @param direction Target direction
     * @return The angle that will take an object towards specified direction
     */
    def apply(direction: Direction2D) = direction match {
        case Direction2D.Right => right
        case Direction2D.Down => down
        case Direction2D.Left => left
        case Direction2D.Up => up
    }
    @deprecated("Renamed to .apply(Direction2D)", "v3.6")
    def towards(direction: Direction2D) = apply(direction)
    
    /**
      * @param angles A number of angles
      * @return An average between the angles
      */
    def average(angles: Iterable[Angle]) = {
        if (angles.isEmpty)
            zero
        else {
            val start = angles.head
            val averageRotation = DirectionalRotation.average(angles.map { _ - start })
            start + averageRotation
        }
    }
}

/**
 * This class is used for storing a double value representing an angle (0 to 2*Pi radians). This 
 * class makes sure the angle stays in bounds and can be operated properly. Please note that 
 * Angle does NOT represent rotation and doesn't have a sign.
 * @author Mikko Hilpinen
 * @since Genesis 30.6.2017
 */
case class Angle private(radians: Double)
    extends LinearSizeAdjustable[Angle] with Combinable[DirectionalRotation, Angle] with SelfComparable[Angle]
        with ValueConvertible with ApproxSelfEquals[Angle]
{
    // ATTRIBUTES    ------------------
    
    /**
      * This angle in degrees. Between 0 and 360.
      */
    lazy val degrees = radians.toDegrees
    
    
    // COMPUTED PROPERTIES    --------
    
    /**
      * @return This angle as a ratio of a full circle [0, 1[
      */
    def circleRatio = radians / (2 * math.Pi)
    
    /**
      * @return A clockwise rotation that will turn an item from 0 radians to this angle
      */
    @deprecated("Please use .toRotation.clockwise instead", "v1.5")
    def toClockwiseRotation = Rotation.clockwise.radians(radians)
    /**
      * @return A non-directional rotation that will turn an item from 0 radians to this angle
      */
    def toRotation = Rotation.radians(radians)
    /**
      * @return A rotation that will turn an item from 0 radians to this angle,
      *         using the shorter route / direction.
      *         Zero rotation is clockwise, 180 degree rotation is counter-clockwise
      */
    def toShortestRotation = {
        if (radians < math.Pi)
            toRotation.clockwise
        else
            Rotation.counterclockwise.radians(math.Pi * 2 - radians)
    }
    
    /**
      * @return Sine of this angle
      */
    def sine = math.sin(radians)
    /**
      * @return Arc sine of this angle
      */
    def arcSine = math.asin(radians)
    /**
      * @return Cosine of this angle
      */
    def cosine = math.cos(radians)
    /**
      * @return Arc cosine of this angle
      */
    def arcCosine = math.acos(radians)
    /**
      * @return Tangent (tan) of this angle
      */
    def tangent = math.tan(radians)
    /**
      * @return Arc tangent (atan) of this sine
      */
    def arcTangent = math.atan(radians)
    
    
    // IMPLEMENTED  ------------------
    
    override def self = this
    
    override implicit def toValue: Value = new Value(Some(this), AngleType)
    override def toString = f"$degrees%1.2f degrees"
    
    override implicit def equalsFunction: EqualsFunction[Angle] = Angle.approxEquals
    
    override def compareTo(o: Angle) = radians.compareTo(o.radians)
    
    /**
      * Applies a rotation to this angle
      */
    def +(rotation: DirectionalRotation): Angle = Angle.radians(radians + rotation.clockwise.radians)
    
    /**
      * Multiplies this angle
      * @param mod A multiplier
      * @return A multiplied version of this angle
      */
    def *(mod: Double) = Angle.radians(radians * mod)
    
    
    // OTHER    -----------------------
    
    /**
      * Applies a negative rotation to this angle
      */
    def -(rotation: DirectionalRotation): Angle = Angle.radians(radians - rotation.clockwise.radians)
    
    
    // OTHER    ------------------
    
    /**
     * The necessary rotation from the other angle to the this angle. Returns the shortest 
     * route, which means that the value is always between -Pi and Pi
     */
    def -(other: Angle) = {
        val rawValue = radians - other.radians
        if (rawValue > math.Pi) {
            // > 180 degrees positive -> < 180 degrees negative
            Rotation.counterclockwise.radians(2 * math.Pi - rawValue)
        }
        else if (rawValue < -math.Pi) {
            // > 180 degrees negative -> < 180 degrees positive
            Rotation.clockwise.radians(rawValue + 2 * math.Pi)
        }
        else {
            // Negative values are returned as positive counter-clockwise rotation
            if (rawValue < 0)
                Rotation.counterclockwise.radians(-rawValue)
            else
                Rotation.clockwise.radians(rawValue)
        }
    }
    /**
     * @param zero The angle that is considered to be the zero angle
     * @return This angle in the coordinate system where the specified angle is considered zero
     */
    def relativeTo(zero: Angle) = Angle.radians(radians - zero.radians)
    
    /**
      * @param other Another angle (> 0 degrees)
      * @return How many times this angle is greater than the other angle
      */
    def /(other: Angle) = radians / other.radians
}