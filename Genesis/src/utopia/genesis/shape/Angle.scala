package utopia.genesis.shape

import utopia.flow.util.{Equatable, RichComparable}
import utopia.genesis.util.Extensions._
import utopia.genesis.util.ApproximatelyEquatable
import utopia.genesis.shape.RotationDirection.Counterclockwise
import utopia.genesis.shape.RotationDirection.Clockwise
import utopia.genesis.shape.shape2D.Direction2D

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
    
    
    // FACTORIES    ------------------------------
    
    /**
     * Converts a radian angle to an angle instance
     */
    def ofRadians(radians: Double) = new Angle(radians)
    
    /**
     * Converts a degrees angle to an angle instance (some inaccuracy may occur since the value 
     * is converted to radians internally)
     */
    def ofDegrees(degrees: Double) = new Angle(degrees.toRadians)
    
    
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
}

/**
 * This class is used for storing a double value representing an angle (0 to 2*Pi radians). This 
 * class makes sure the angle stays in bounds and can be operated properly. Please note that 
 * Angle does NOT represent rotation.
 * @author Mikko Hilpinen
 * @since 30.6.2017
 */
class Angle(rawRadians: Double) extends Equatable with ApproximatelyEquatable[Angle] with RichComparable[Angle]
{
    // ATTRIBUTES    ------------------
    
    /**
     * This angle in radians. Between 0 and 2*Pi
     */
    val toRadians = { val downscaled = rawRadians % (2 * math.Pi)
        if (downscaled < 0) downscaled + 2 * math.Pi else downscaled }
    
    
    // COMPUTED PROPERTIES    --------
    
    /**
     * This angle in degrees. between 0 and 360
     */
    lazy val toDegrees = toRadians.toDegrees
    
    override def properties = Vector(toRadians)
    
    override def toString = f"$toDegrees%1.2f degrees"
    
    /**
      * @return A rotation that will turn an item from 0 radians to this angle
      */
    def toRotation = Rotation(toRadians)
    
    
    // IMPLEMENTED  ------------------
    
    override def compareTo(o: Angle) = ((toRadians - o.toRadians) * 1000).toInt
    
    
    // OPERATORS    ------------------
    
    /**
     * The necessary rotation from the other angle to the this angle. Returns the shortest 
     * route, which means that the value is always between -Pi and Pi
     */
    def -(other: Angle) = 
    {
        val rawValue = toRadians - other.toRadians
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
    def +(rotationRads: Double) = Angle.ofRadians(toRadians + rotationRads)
    
    /**
     * Applies a rotation to this angle
     */
    def +(rotation: Rotation): Angle = this + rotation.toDouble
    
    /**
     * Applies a rotation (radians) to this angle in counter-clockwise direction
     */
    def -(rotationRads: Double) = this + (-rotationRads)
    
    /**
     * Applies a negative rotation to this angle
     */
    def -(rotation: Rotation): Angle = this.-(rotation.toDouble)
    
    /**
     * Compares two angles without the requirement of being exactly equal
     */
    def ~==(other: Angle) = toRadians ~== other.toRadians
    
    /**
      * Multiplies this angle
      * @param mod A multiplier
      * @return A multiplied version of this angle
      */
    def *(mod: Double) = Angle.ofRadians(toRadians * mod)
    
    /**
      * Divides this angle
      * @param div A divider
      * @return A divided version of this angle
      */
    def /(div: Double) = this * (1/div)
}