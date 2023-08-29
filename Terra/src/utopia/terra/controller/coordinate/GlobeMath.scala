package utopia.terra.controller.coordinate

import utopia.paradigm.measurement.Distance
import utopia.paradigm.measurement.DistanceUnit.KiloMeter

/**
 * Contains constants relating to geometric mathematics concerning the Earth.
 * These are mostly assumed values based on mathematical calculations.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
object GlobeMath
{
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
}
