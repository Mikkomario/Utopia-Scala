package utopia.terra.controller.coordinate

import utopia.paradigm.angular.Rotation
import utopia.paradigm.measurement.Distance
import utopia.paradigm.measurement.DistanceUnit.KiloMeter
import utopia.paradigm.shape.shape2d.vector.Vector2D

/**
 * Contains constants relating to geometric mathematics concerning the Earth.
 * These are mostly assumed values based on mathematical calculations.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
object GlobeMath
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * The Earth's supposed radius at the equator: 6,335.439 km
	 * Source: https://en.wikipedia.org/wiki/Earth_radius [29.8.2023]
	 */
	val earthRadiusAtEquator = Distance(6335.439, KiloMeter)
	/**
	 * The Earth's supposed radius under the poles: 6,399.594 km
	 * Source: https://en.wikipedia.org/wiki/Earth_radius [29.8.2023]
	 */
	val earthRadiusAtPoles = Distance(6399.594, KiloMeter)
	/**
	 * The Earth arithmetic mean radius, 6,371.0088 km.
	 * Value used typically when assuming perfectly spherical Earth.
	 * Source: https://en.wikipedia.org/wiki/Earth_radius [29.8.2023]
	 */
	val meanRadius = Distance(6371.0088, KiloMeter)
	
	/**
	  * The arithmetic Earth mean circumference, which is based on the
	  * [[meanRadius]] value
	  */
	lazy val meanCircumference = meanRadius * 2.0 * math.Pi
	
	
	// OTHER    --------------------
	
	/**
	  * Calculates the radius along the east-west -plane at a certain latitude level.
	  * @param latitude The targeted latitude level, presented as [[Rotation]],
	  *                 where counter-clockwise is towards the north and
	  *                 clockwise is towards the south.
	  * @param globeVectorRadius The radius of the earth in the vector measurement system.
	  * @return The east-west circle radius at the specified latitude level +
	  *         the vector length traveled along the Z axis to reach that level.
	  *         These are returned as a Pair.
	  */
	def eastWestRadiusAtLatitude(latitude: Rotation, globeVectorRadius: Double) = {
		// Calculates the position on the X-Z plane based on latitude, which determines the east-west radius,
		// as well as the final Z-coordinate
		// X is perpendicular to the "pole" vector
		// While Y is parallel to the "pole" vector, going up to north
		val xz = Vector2D.lenDir(globeVectorRadius, latitude.toAngle)
		xz.xyPair
	}
}
