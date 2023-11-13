package utopia.terra.model.world

import utopia.flow.operator.EqualsBy
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorLike}
import utopia.terra.model.angular.LatLongRotation

/**
  * Common trait for world point classes that provide operative functions and use double number vectors
  * @author Mikko Hilpinen
  * @since 12.11.2023, v1.1
  * @tparam V Type of vectors used by this world point type
  * @tparam P Thea actual type of implementing points. Also used in path creation.
  * @tparam Aerial Type of the "aerial" versions of this point
  * @tparam T Type of paths created between these points
  */
trait DoubleWorldPointOps[+V <: DoubleVectorLike[V], P, +Aerial, +T]
	extends WorldPointOps[V, P, DoubleVector, Aerial, T] with EqualsBy
{
	// ABSTRACT ----------------------
	
	/**
	  * @param location The new location to assign.
	  *                 Based on the existing vector form of this point, should contain the same number of
	  *                 dimensions and likely be of the same type.
	  * @return Copy of this point at the specified location
	  */
	protected def at(location: HasDoubleDimensions): P
	
	
	// IMPLEMENTED  ------------------
	
	override protected def equalsProperties: Iterable[Any] = Vector(vector, worldView)
	
	override def +(other: LatLongRotation): P = at(latLong + other)
	override def +(vectorTravel: DoubleVector): P = at(vector + vectorTravel)
}
