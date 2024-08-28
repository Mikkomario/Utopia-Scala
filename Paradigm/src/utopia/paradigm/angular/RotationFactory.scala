package utopia.paradigm.angular

/**
  * Common trait for factories used for constructing Rotation instances of
  * some kind
  * @author Mikko Hilpinen
  * @since 10.11.2023 v1.5
  * @tparam R The type of rotation instances created
  */
trait RotationFactory[+R]
{
	// ABSTRACT --------------------
	
	/**
	  * Creates a new rotation instance
	  * @param rads Amount of radians to rotate
	  */
	def radians(rads: Double): R
	
	
	// COMPUTED --------------------
	
	/**
	  * A zero rotation
	  */
	def zero = radians(0.0)
	/**
	  * A full 360 degrees rotation
	  */
	def circle = circles(1)
	/**
	  * A 90 degrees rotation
	  */
	def quarter = circles(0.25)
	/**
	  * @return A 180 degree rotation
	  */
	def halfCircle = circles(0.5)
	/**
	  * @return A 270 degree rotation
	  */
	def threeQuarters = circles(0.75)
	/**
	  * @return A full 360 degree rotation
	  */
	def revolution = circles(1.0)
	
	
	// OTHER	--------------------------
	
	/**
	  * Converts a degree amount to a rotation
	  */
	def degrees(degrees: Double) = radians(degrees.toRadians)
	/**
	  * @param circles   The number of full circles (360 degrees or 2Pi radians) rotated
	  * @return A new rotation
	  */
	def circles(circles: Double) = radians(circles * 2.0 * math.Pi)
	/**
	  * @param quarters The number of 90 degree quarters rotated
	  * @return A new rotation
	  */
	def quarters(quarters: Double) = radians(quarters * 0.5 * math.Pi)
	
	/**
	  * @param arcLength    Targeted arc length (NB: May be negative)
	  * @param circleRadius Radius of the applicable circle
	  * @return Rotation that is required for producing the specified arc length over the specified travel radius.
	  *         The resulting rotation is negative / towards the opposite direction,
	  *         if the specified arc length is negative.
	  */
	def forArcLength(arcLength: Double, circleRadius: Double) = {
		// Whole circle diameter is 2*Pi*r. Length of the arc is (a / 2*Pi) * r, where a is the targeted angle
		// Therefore a = l/r, where l is the desired arc length
		radians(arcLength / circleRadius)
	}
}

