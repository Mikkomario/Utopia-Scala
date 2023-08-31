package utopia.terra.controller.coordinate.map

import utopia.paradigm.shape.shape3d.{Matrix3D, Vector3D}
import utopia.terra.model.map.MapPoint

/**
  * Used for converting 3D vector locations to points on a 2D map
  * @author Mikko Hilpinen
  * @since 31.8.2023, v1.0
  */
class PointMap3D(override val origin: MapPoint[Vector3D],
                 reference1: MapPoint[Vector3D], reference2: MapPoint[Vector3D], reference3: MapPoint[Vector3D])
	extends PointMapLike[Vector3D, Matrix3D]
{
	private val references = Vector(reference1, reference2, reference3)
	private val transforms = references.map { _ - origin }
	
	override protected val vectorTransform: Matrix3D = Matrix3D(transforms.map { _.vector }).inverse
		.getOrElse { throw new IllegalArgumentException(
			"The specified coordinates can't be used to produce a 3-dimensional space") }
	// Accepts vector products (over reference vectors) and returns an image coordinate
	private val imageTransform = Matrix3D(transforms.map { _.mapLocation.toVector3D })
	override protected val fullTransform: Matrix3D = imageTransform * vectorTransform
}
