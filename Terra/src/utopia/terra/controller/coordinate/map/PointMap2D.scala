package utopia.terra.controller.coordinate.map

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.terra.model.map.MapPoint

/**
  * Used for converting 2D vectors to map image coordinates
  * @author Mikko Hilpinen
  * @since 31.8.2023, v1.0
  */
class PointMap2D(override val origin: MapPoint[Vector2D], references: Pair[MapPoint[Vector2D]])
	extends PointMapLike[Vector2D, Matrix2D]
{
	// ATTRIBUTES   -------------------------
	
	private val transforms = references.map { _ - origin }
	
	override protected val vectorTransform: Matrix2D = Matrix2D(transforms.map { _.vector }).inverse
		.getOrElse { throw new IllegalArgumentException(
			"The specified reference vectors can't produce a two-dimensional space") }
	// Transforms that accepts a vector product (projection over the reference vectors)
	// and produces an image coordinate (relative to 'origin')
	private val imageTransform = Matrix2D(transforms.map { _.mapLocation.toVector })
	override protected val fullTransform: Matrix2D = imageTransform * vectorTransform
}
