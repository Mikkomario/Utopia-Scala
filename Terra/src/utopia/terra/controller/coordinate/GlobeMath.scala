package utopia.terra.controller.coordinate

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.measurement.Distance
import utopia.paradigm.measurement.DistanceUnit.{KiloMeter, Meter}
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.terra.controller.coordinate.world.{SphericalEarth, VectorDistanceConversion}
import utopia.terra.model.angular.{LatLong, NorthSouthRotation}
import utopia.terra.model.world.WorldDistance
import utopia.terra.model.world.sphere.SpherePoint

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
	def eastWestRadiusAtLatitude(latitude: NorthSouthRotation, globeVectorRadius: Double) = {
		// Calculates the position on the X-Z plane based on latitude, which determines the east-west radius,
		// as well as the final Z-coordinate
		// X is perpendicular to the "pole" vector
		// While Y is parallel to the "pole" vector, going up to north
		val xz = Vector2D.lenDir(globeVectorRadius, latitude.toAngle)
		xz.xyPair
	}
	
	/**
	  * Calculates the haversine (i.e. arcing) distance between two aerial points
	  * @param points Surface point locations to compare
	  * @param altitudes Altitudes to compare
	  * @param radius Radius to the mean sea level / 0 altitude
	  * @param conversion Implicit conversion for converting distances into world distances
	  * @return Distance between the two points
	  */
	def haversineDistanceBetween(points: Pair[LatLong], altitudes: Pair[WorldDistance], radius: WorldDistance)
	                            (implicit conversion: VectorDistanceConversion): WorldDistance =
	{
		// Uses a mean radius, giving a greater emphasis on the higher radius value
		// (2a + b) / 3, where a is the higher radius and b is the lower radius
		val meanRadius = radius + (altitudes.first * 2 + altitudes.second) / 3.0
		// Calculates the haversine (arc) distance between the two points at the mean radius
		val arcLength = haversineDistanceBetween(points, meanRadius)
		
		// Adds slight accounting for the vertical travel, also
		// Uses the Pythagorean theorem
		val verticalDistance = altitudes.merge { _ - _ }.abs
		
		if (verticalDistance.isZero)
			arcLength
		else if (arcLength.isZero)
			verticalDistance
		else {
			import math._
			Distance(sqrt(pow(arcLength.toM, 2) + pow(verticalDistance.toM, 2)), Meter)
		}
	}
	/**
	  * Calculates the haversine (i.e. arcing) distance between two surface points over a perfectly spherical earth
	  * @param points The points to compare
	  * @param radius Assumed earth radius
	  * @return The travel distance between the two points
	  */
	def haversineDistanceBetween(points: Pair[LatLong], radius: WorldDistance) = {
		import math._
		
		val latitudes = points.map { _.latitude.unidirectional.radians }
		val longitudes = points.map { _.longitude.radians }
		
		val deltaLat = latitudes.merge { _ - _ }
		val deltaLon = longitudes.merge { _ - _ }
		
		val k = pow(sin(deltaLat / 2), 2) + latitudes.mapAndMerge(cos) { _ * _ } * pow(sin(deltaLon / 2), 2)
		val c = 2 * atan2(sqrt(k), sqrt(1 - k))
		
		radius * c
	}
	
	/**
	  * Calculates, how much of a viewed target should be hidden behind a physical horizon on a spherical earth
	  * @param observer The observer's location. Note: This should be at the height of the observer's eyes / lenses.
	  * @param targetLowestPoint Location of the viewed object's lowest point.
	  *                          E.g. If viewing an object over the sea, this point should be at altitude 0.
	  * @param targetHeight The height of the observed object
	  * @return Returns various results, including how much of the target should be hidden behind the curve
	  */
	def calculateHiddenHeight(observer: SpherePoint, targetLowestPoint: SpherePoint, targetHeight: Distance) = {
		// First, we form a triangle between B (observer), T (target) and O (origin),
		// and calculate the angle at the origin's corner:
		//      cos(a) = (O dot T) / (|O|*|T|)
		//      a = acos((O dot T) / (|O|*|T|))
		//
		// Where a is the angle at origin O.
		val target = targetLowestPoint.soarBy(targetHeight)
		val lenObserver = observer.vector.length
		val lenTarget = target.vector.length
		val mainAngle = observer.vector.angleDifference(target.vector)
		
		// We form a simplified coordinate system, where the observer lies at X=0, and both points are at the Z=0 plane.
		// positive Y direction is towards the observer, in this case
		val observerV = Vector2D(0, lenObserver)
		// We can resolve the target coordinates using the main angle a (X is based on cos and Y is based on sin)
		// Xt = cos(Pi/2-a)*|T|
		// Yt = sin(Pi/2-a)*|T|
		// We use .lenDir to get this same effect
		val compositeMainAngle = (Angle.quarter - mainAngle).toAngle
		val targetV = Vector2D.lenDir(lenTarget, compositeMainAngle)
		
		// Next we calculate the direction of the line-of-sight to the physical horizon,
		// which is a tangent along the earth, reaching O
		//      cos(dH) = R/|O|
		//      dH = acos(R/|O|)
		//      b = Pi/2 - dH
		//      b = Pi/2 - acos(R/|O|)
		//      dOH = b - Pi/2
		//      dOH = (Pi/2 - dH) - Pi/2
		//      dOH = -dH
		//
		// Here, dH is angle from origin to horizon H
		//       b is angle from observer O to horizon H (within the triangle)
		//       dOH is the direction of the line-of-sight in the X-Y coordinate system
		val directionToHorizon = Angle.radians(math.acos(SphericalEarth.globeVectorRadius/lenObserver))
		val lineOfSightDirection = -directionToHorizon
		val unitLineOfSight = Line.fromVector(observerV.toPoint, Vector2D.lenDir(1.0, lineOfSightDirection))
		
		// Also calculates how far the visible horizon should be on the ground level
		// This is simply the arc length of the horizon angle (dH)
		val horizonDistance = SphericalEarth.distanceOf(
			directionToHorizon.toShortestRotation.arcLengthOver(SphericalEarth.globeVectorRadius)).abs
		
		// Also calculates the distance between the observer and the target, along the sphere surface
		val targetDistance = SphericalEarth
			.distanceOf(mainAngle.toShortestRotation.arcLengthOver(SphericalEarth.globeVectorRadius))
		
		// Next we calculate the intersection between a line drawn from the observer to the horizon
		// (representing the lowest visible line-of-sight over the horizon)
		// and the target
		val (hidden, visible) = unitLineOfSight
			.intersection(Line(Point.origin, targetV.toPoint), onlyPointsInSegment = false) match
		{
			case Some(hiddenPoint) =>
				// Calculates how high this (hidden) intersection point is, relative to the target's lowest point
				val targetLowestHeight = targetLowestPoint.vector.length
				val hiddenVectorHeight = (hiddenPoint.length - targetLowestHeight) max 0.0
				val hiddenHeight = SphericalEarth.distanceOf(hiddenVectorHeight)
				val visibleHeight = (targetHeight - hiddenHeight) max Distance.zero
				
				(hiddenHeight, visibleHeight)
			
			// Special case: The observer is looking directly up or down
			case None => (Distance.zero, targetHeight)
		}
		
		HiddenHeightResults(observerV,
			Line.fromVector(targetV.toPoint, targetV.withLength(-SphericalEarth.vectorLengthOf(targetHeight))),
			Vector2D.lenDir(SphericalEarth.globeVectorRadius, directionToHorizon), hidden, visible,
			horizonDistance, targetDistance)
	}
	
	
	// NESTED   -----------------------------
	
	/**
	  * Combines information about target hidden height -calculation
	  * @param observer A vector representing the observer's position
	  * @param target A line representing the target's vertical area, from the lowest to the highest point
	  * @param horizon A point where the observer's line of sight meets the horizon
	  * @param hiddenLength Amount of target that's hidden behind the earth's curvature
	  * @param visibleLength Amount of target that's visible above the earth's curvature
	  * @param distanceToHorizon Distance between the observer and the visible horizon,
	  *                          along the earth's surface at sea level.
	  * @param distanceToTarget Distance between the observer and the viewed target,
	  *                         along the earth's surface at sea level.
	  */
	case class HiddenHeightResults(observer: Vector2D, target: Line, horizon: Vector2D, hiddenLength: Distance,
	                               visibleLength: Distance, distanceToHorizon: Distance, distanceToTarget: Distance)
}
