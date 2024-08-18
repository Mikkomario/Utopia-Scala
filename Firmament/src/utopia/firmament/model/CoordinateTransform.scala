package utopia.firmament.model

import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.template.vector.DoubleVectorLike

/**
  * Represents a coordinate transformation that may be applied between component layers,
  * or elsewhere.
  * @author Mikko Hilpinen
  * @since 17.08.2024, v1.3.1
  */
trait CoordinateTransform
{
	/**
	  * Applies this transformation to a coordinate (transforming it from "absolute" to "relative" space)
	  * @param coordinate Coordinate to transform
	  * @return Matching coordinate in the transformed space
	  */
	def apply[V <: DoubleVectorLike[V]](coordinate: V): V
	
	/**
	  * Applies the inverse of this transformation to a coordinate
	  * (transforming it from "relative" to "absolute" space)
	  * @param coordinate A transformed coordinate to revert
	  * @return Matching coordinate in the non-transformed space.
	  *         None if it is impossible to invert this transformation
	  *         (because dimensions are lost)
	  */
	def invert[V <: DoubleVectorLike[V]](coordinate: V): V
	/**
	  * Applies the inverse of this transformation to a set of bounds
	  * (transforming it from "relative" to "absolute" space)
	  * @param area Transformed bounds to revert
	  * @return Matching area in the non-transformed space.
	  *         None if it is impossible to invert this transformation
	  *         (because dimensions are lost)
	  */
	def invert(area: Bounds): Bounds
}
