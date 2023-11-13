package utopia.terra.model.world.sphere

import utopia.flow.operator.equality.EqualsBy
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.terra.controller.coordinate.world.{SphericalEarth, WorldPointFactory}
import utopia.terra.model.CompassTravel
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.{AerialPoint, DoubleWorldPointOps, WorldDistance}

object SpherePoint
	extends WorldPointFactory[Vector3D, Vector3D, SpherePoint, SpherePoint]
{
	// IMPLEMENTED  ------------------------
	
	override protected implicit def worldView: SphericalEarth.type = SphericalEarth
	
	override def apply(latLong: LatLong): SpherePoint = apply(latLong, WorldDistance.zero)
	override def apply(latLong: LatLong, altitude: WorldDistance): SpherePoint = new LatLongOverSphere(latLong, altitude)
	
	override def surfaceVector(vector: Vector3D): SpherePoint = apply(vector).toSurfacePoint
	override def aerialVector(vector: Vector3D): SpherePoint = apply(vector)
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param vector A vector in the spherical Earth system
	 * @return A point matching that vector
	 */
	def apply(vector: Vector3D): SpherePoint = new VectorOverSphere(vector)
	
	
	// NESTED   ----------------------------
	
	class VectorOverSphere(override val vector: Vector3D) extends SpherePoint
	{
		// ATTRIBUTES   --------------------
		
		private val lazyLatLong = Lazy { SphericalEarth.vectorToLatLong(vector) }
		private val lazyAltitude = Lazy { SphericalEarth.altitudeAt(vector) }
		
		
		// IMPLEMENTED  --------------------
		
		override def latLong: LatLong = lazyLatLong.value
		override def altitude: WorldDistance = lazyAltitude.value
		
		override def toSurfacePoint = if (lazyAltitude.current.exists { _.isZero }) this else withAltitude(0.0)
		
		override def withAltitude(altitude: Distance): SpherePoint = lazyLatLong.current match {
			case Some(ll) => SpherePoint(ll, worldView.distance(altitude))
			case None => copyWithAltitude(worldView.vectorLengthOf(altitude))
		}
		override def withAltitude(altitude: Double): SpherePoint = lazyLatLong.current match {
			case Some(ll) => SpherePoint(ll, worldView.distance(altitude))
			case None => copyWithAltitude(altitude)
		}
		
		
		// OTHER    -----------------------
		
		private def copy(newVector: Vector3D) =
			if (newVector == vector) this else new VectorOverSphere(newVector)
			
		private def copyWithAltitude(altitude: Double) =
			copy(vector.withLength(1.0 + altitude / worldView.globeVectorRadius))
	}
	
	class LatLongOverSphere(override val latLong: LatLong, override val altitude: WorldDistance) extends SpherePoint
	{
		// ATTRIBUTES   ------------------
		
		override lazy val vector: Vector3D = worldView.latLongToVector(latLong, altitude.vectorLength)
		
		
		// IMPLEMENTED  ------------------
		
		override def toSurfacePoint: SpherePoint = _withAltitude(WorldDistance.zero)
		
		override def withAltitude(altitude: Distance): SpherePoint = _withAltitude(altitude)
		override def withAltitude(altitude: Double): SpherePoint = _withAltitude(altitude)
		
		
		// OTHER    ---------------------
		
		private def _withAltitude(altitude: WorldDistance) =
			if (this.altitude == altitude) this else new LatLongOverSphere(latLong, altitude)
	}
}

/**
 * Represents a specific location in the spherical Earth system.
 * See [[SphericalEarth]] for details about the coordinate system used.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
trait SpherePoint
	extends AerialPoint[Vector3D, SpherePoint, DoubleVector, SpherePoint, SphereTravel]
		with DoubleWorldPointOps[Vector3D, SpherePoint, SpherePoint, SphereTravel] with EqualsBy
{
	override protected implicit def worldView: SphericalEarth.type = SphericalEarth
	
	override def at(latLong: LatLong): SpherePoint = SpherePoint(latLong, altitude)
	override protected def at(location: HasDoubleDimensions): SpherePoint = SpherePoint(Vector3D.from(location))
	
	override def +(travel: CompassTravel) = {
		// Converts the travel distance to rotation
		this + travel.compassAxis.rotation.forArcLength(worldView.vectorLengthOf(travel.distance), vector.length)
	}
	
	override def to(target: SpherePoint): SphereTravel = SphereTravel(this, target)
	override def -(origin: SpherePoint): SphereTravel = SphereTravel(origin, this)
}
