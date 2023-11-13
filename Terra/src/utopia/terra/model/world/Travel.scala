package utopia.terra.model.world

import utopia.flow.operator.Reversible
import utopia.paradigm.path.Path
import utopia.paradigm.shape.template.vector.NumericVectorLike
import utopia.terra.controller.coordinate.world.VectorDistanceConversion
import utopia.terra.model.angular.LatLongRotation

/**
  * Common trait for models that represent travel between two world points
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  * @tparam D Type of vector dimensions used
  * @tparam P Type of the world points used
  * @tparam V Type of vector representations used
  * @tparam VI Type of comparable (high level input) vectors that are accepted by this class
  */
trait Travel[D, P <: WorldPointOps[V, P, VI, _, _], +V <: NumericVectorLike[D, V, V] with VI, VI <: Reversible[VI]]
	extends Path[P]
{
	// ABSTRACT -----------------
	
	/**
	  * @return The world view used / assumed in this travel
	  */
	implicit protected def worldView: VectorDistanceConversion
	
	/**
	  * @return The amount of altitude gained during this travel.
	  *         Negative value if altitude is lost.
	  */
	def altitudeIncrease: WorldDistance
	
	/**
	  * @return The length of this travel in "real world" distance,
	  *         assuming arcing travel, if applicable.
	  */
	def arcingDistance: WorldDistance
	
	/**
	  * Calculates a point on this path, assuming arcing travel (if applicable)
	  * @param progress The progress on this travel, where
	  *                 0 means no travel (from the [[start]]) and 1.0 means
	  *                 arrival to the [[end]].
	  *                 E.g. 0.5 would return a point at the middle of this travel.
	  * @return A point on this path that matches the specified progress
	  */
	def arcingProgress(progress: Double): P
	
	
	// COMPUTED ----------------
	
	/**
	  * @return A vector representation of this travel
	  */
	def vector: V = end.vector - start.vector
	/**
	  * @return An angular rotation representation of this travel
	  *         (i.e. latitude and longitude change)
	  */
	def rotation: LatLongRotation = end.latLong - start.latLong
	
	/**
	  * @return Whether this travel doesn't involve any altitude change
	  */
	def isSurfaceTravel = altitudeIncrease.isZero
	/**
	  * @return Whether this travel involves altitude change
	  */
	def isAerialTravel = !isSurfaceTravel
	
	/**
	  * @return The length of this travel in "real world" distance,
	  *         assuming linear travel
	  */
	def linearDistance: WorldDistance = worldView.distanceOf(vector.length)
	
	/**
	  * @return The point at the middle of this travel path
	  */
	def middlePoint = apply(0.5)
	
	
	// OTHER    ----------------
	
	/**
	  * Calculates a point on this path that matches the specified 'progress'
	  * @param progress The progress on this travel, where
	  *                 0 means no travel (from the [[start]]) and 1.0 means
	  *                 arrival to the [[end]].
	  *                 E.g. 0.5 would return a point at the middle of this travel.
	  * @param isArcing Whether the travel should be assumed to be arcing (true)
	  *                 or linear (false)
	  * @return Point on this travel path that matches the specified progress
	  */
	def apply(progress: Double, isArcing: Boolean): P = {
		// Delegates the arcing travel calculation to a separate function, if applicable
		if (isArcing) arcingProgress(progress) else linearProgress(progress)
	}
	
	/**
	  * Calculates a point on this travel path, assuming linear travel
	  * @param progress The progress on this travel, where
	  *                 0 means no travel (from the [[start]]) and 1.0 means
	  *                 arrival to the [[end]].
	  *                 E.g. 0.5 would return a point at the middle of this travel.
	  * @return A point on this travel path that matches the specified progress.
	  */
	def linearProgress(progress: Double): P = {
		// Handles the special use-cases
		if (progress == 0.0)
			start
		else if (progress == 1.0)
			end
		else
			start + vector.scaledBy(progress)
	}
	
	/**
	  * Calculates a point on this path, assuming arcing travel (if applicable).
	  * Assumes that there is no altitude shift applicable to this travel.
	  * @param progress     The progress on this travel, where
	  *                     0 means no travel (from the [[start]]) and 1.0 means
	  *                     arrival to the [[end]].
	  *                     E.g. 0.5 would return a point at the middle of this travel.
	  * @return A point on this path that matches the specified progress
	  */
	protected def arcingProgress2D(progress: Double): P = {
		// Case: Targeting start
		if (progress == 0.0)
			start
		// Case: Targeting end
		else if (progress == 1.0)
			end
		// Case: Targeting some other point => Uses angle-based travel
		else
			start + rotation * progress
	}
	/**
	  * Calculates a point on this path, assuming arcing travel (if applicable).
	  * Assumes that this travel may contain an altitude element.
	  * @param progress The progress on this travel, where
	  *                 0 means no travel (from the [[start]]) and 1.0 means
	  *                 arrival to the [[end]].
	  *                 E.g. 0.5 would return a point at the middle of this travel.
	  * @param gainAltitude A function for applying an altitude change to a (laterally-shifted) point
	  * @return A point on this path that matches the specified progress
	  */
	protected def arcingProgress3D[P2 >: P](progress: Double)(gainAltitude: (P, WorldDistance) => P2): P2 = {
		// Case: Targeting start
		if (progress == 0.0)
			start
		// Case: Targeting end
		else if (progress == 1.0)
			end
		// Case: Targeting some other point => Uses angle-based travel and applies altitude increase on top of that
		else {
			val laterallyMoved = start + rotation * progress
			val ai = altitudeIncrease
			// Case: No altitude gain => Returns the laterally shifted point
			if (ai.isZero)
				laterallyMoved
			// Case: Altitude gain => Applies the correct level of altitude shift
			else
				gainAltitude(laterallyMoved, ai * progress)
		}
	}
}
