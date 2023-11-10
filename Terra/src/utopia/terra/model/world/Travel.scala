package utopia.terra.model.world

import utopia.flow.operator.Reversible
import utopia.paradigm.path.Path
import utopia.paradigm.shape.template.HasDimensions
import utopia.paradigm.shape.template.vector.NumericVectorLike
import utopia.terra.controller.coordinate.world.VectorDistanceConversion
import utopia.terra.model.angular.LatLongRotation

/**
  * Common trait for models that represent travel between two world points
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  * @tparam P Type of the world points used
  * @tparam V Type of vector representations used
  */
trait Travel[D, +P <: WorldPointOps[V, _, VI, P], VI <: HasDimensions[D] with Reversible[VI],
	V <: NumericVectorLike[D, V, V] with VI]
	extends Path[P]
{
	// ABSTRACT -----------------
	
	/**
	  * @return The world view used / assumed in this travel
	  */
	implicit protected def worldView: VectorDistanceConversion
	
	/**
	  * @return The length of this travel in "real world" distance,
	  *         assuming arcing travel, if applicable.
	  */
	def arcingDistance: WorldDistance
	
	/**
	  * @param vector A vector position
	  * @return A world point that matches the specified vector position
	  */
	protected def pointAt(vector: V): P
	
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
}
