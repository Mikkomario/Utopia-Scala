package utopia.paradigm.angular

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConvertible
import utopia.flow.operator.{Combinable, LinearScalable}
import utopia.flow.util.SelfComparable
import utopia.flow.operator.EqualsExtensions._
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.generic.AngleType

object Angle
{
    // ATTRIBUTES    -----------------------------
    
    /**
     * Angle that points to the left (180 degrees)
     */
    val left = ofRadians(math.Pi)
    /**
     * Angle that points to the right (0 degrees)
     */
    val right = ofRadians(0)
    /**
     * Angle that points up (270 degrees)
     */
    val up = ofRadians(3 * math.Pi / 2)
    /**
     * Angle that points down (90 degrees)
     */
    val down = ofRadians(math.Pi / 2)
    
    /**
      * The red color angle when using HSL
      */
    val red = ofRadians(0)
    /**
      * The green color angle when using HSL
      */
    val green = ofRadians(2 * math.Pi / 3)
    /**
      * The blue color angle when using HSL
      */
    val blue = ofRadians(4 * math.Pi / 3)
    
    
    // COMPUTED ----------------------------------
    
    /**
      * @return A zero degree angle
      */
    def zero = right
    
    /**
      * @return 1/4 of a circle
      */
    def quarter = down
    /**
      * @return 1/2 of a circle
      */
    def half = left
    /**
      * @return 3/4 of a circle
      */
    def threeQuarters = up
    
    
    // FACTORIES    ------------------------------
    
    /**
     * Converts a radian angle to an angle instance
     */
    def ofRadians(radians: Double) =
    {
        val raw = radians % (2 * math.Pi)
        if (raw < 0)
            Angle(raw + 2 * math.Pi)
        else
            Angle(raw)
    }
    /**
     * Converts a degrees angle to an angle instance (some inaccuracy may occur since the value 
     * is converted to radians internally)
     */
    def ofDegrees(degrees: Double) = ofRadians(degrees.toRadians)
    
    
    // OTHER    ----------------------------------
    
    /**
     * @param direction Target direction
     * @return The angle that will take an object towards specified direction
     */
    def towards(direction: Direction2D) = direction match
    {
        case Direction2D.Right => right
        case Direction2D.Down => down
        case Direction2D.Left => left
        case Direction2D.Up => up
    }
    
    /**
      * @param angles A number of angles
      * @return An average between the angles
      */
    def average(angles: Iterable[Angle]) =
    {
        if (angles.isEmpty)
            zero
        else
        {
            val start = angles.head
            val averageRotation = Rotation.average(angles.map { _ - start })
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
    extends LinearScalable[Angle] with Combinable[Angle, Rotation] with SelfComparable[Angle] with ValueConvertible
{
    // ATTRIBUTES    ------------------
    
    /**
      * This angle in degrees. Between 0 and 360.
      */
    lazy val degrees = radians.toDegrees
    
    
    // COMPUTED PROPERTIES    --------
    
    def circles = radians / (2 * math.Pi)
    
    /**
      * This angle in radians. Between 0 and 2*Pi
      */
    @deprecated("Please use .radians instead", "v2.3")
    def toRadians = radians /*{ val downscaled = rawRadians % (2 * math.Pi)
        if (downscaled < 0) downscaled + 2 * math.Pi else downscaled }*/
    
    /**
     * This angle in degrees. between 0 and 360
     */
    @deprecated("Please use .degrees instead", "v2.3")
    def toDegrees = degrees
    
    /**
      * @return A rotation that will turn an item from 0 radians to this angle
      */
    def toRotation = Rotation(radians)
    
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
    
    override def repr = this
    
    override implicit def toValue: Value = new Value(Some(this), AngleType)
    
    override def toString = f"$degrees%1.2f degrees"
    
    override def compareTo(o: Angle) = ((radians - o.radians) * 10000).toInt
    
    /**
      * Applies a rotation to this angle
      */
    def +(rotation: Rotation): Angle = Angle.ofRadians(radians + rotation.clockwiseRadians)
    
    /**
      * Applies a rotation (radians) to this angle in counter-clockwise direction
      */
    @deprecated("Please use -(Rotation) instead", "v2.3")
    def -(rotationRads: Double) = Angle.ofRadians(radians - rotationRads)
    
    /**
      * Applies a negative rotation to this angle
      */
    def -(rotation: Rotation): Angle = Angle.ofRadians(radians - rotation.clockwiseRadians)
    
    /**
      * Compares two angles without the requirement of being exactly equal
      */
    def ~==(other: Angle) = radians ~== other.radians
    
    /**
      * Multiplies this angle
      * @param mod A multiplier
      * @return A multiplied version of this angle
      */
    def *(mod: Double) = Angle.ofRadians(radians * mod)
    
    
    // OPERATORS    ------------------
    
    /**
     * The necessary rotation from the other angle to the this angle. Returns the shortest 
     * route, which means that the value is always between -Pi and Pi
     */
    def -(other: Angle) = 
    {
        val rawValue = radians - other.radians
        if (rawValue > math.Pi)
        {
            // > 180 degrees positive -> < 180 degrees negative
            Rotation(2 * math.Pi - rawValue, Counterclockwise)
        }
        else if (rawValue < -math.Pi)
        {
            // > 180 degrees negative -> < 180 degrees positive
            Rotation(rawValue + 2 * math.Pi, Clockwise)
        }
        else
        {
            // Negative values are returned as positive counter-clockwise rotation
            if (rawValue < 0)
                Rotation(-rawValue, Counterclockwise)
            else
                Rotation(rawValue, Clockwise)
        }
    }
    
    /**
     * Applies a rotation (radians) to this angle in clockwise direction
     */
    @deprecated("Please use +(Rotation) instead", "v2.3")
    def +(rotationRads: Double) = Angle.ofRadians(radians + rotationRads)
    
    /**
      * @param other Another angle (> 0 degrees)
      * @return How many times this angle is greater than the other angle
      */
    def /(other: Angle) = radians / other.radians
}