package utopia.paradigm.transform

import utopia.paradigm.animation.Animation
import utopia.paradigm.animation.transform.{AnimatedAffineTransformable, AnimatedLinearTransformable}
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape3d.Matrix3D

/**
  * A combination trait of various transformable traits
  * @author Mikko Hilpinen
  * @since Genesis 26.12.2020, v2.4
  */
trait Transformable[+Transformed] extends LinearTransformable[Transformed] with AffineTransformable[Transformed]
	with AnimatedLinearTransformable[Animation[Transformed]] with AnimatedAffineTransformable[Animation[Transformed]]
{
	override def transformedWith(transformation: Animation[Matrix2D]) =
		transformation.map(transformedWith)
	
	override def affineTransformedWith(transformation: Animation[Matrix3D]) =
		transformation.map(transformedWith)
}
