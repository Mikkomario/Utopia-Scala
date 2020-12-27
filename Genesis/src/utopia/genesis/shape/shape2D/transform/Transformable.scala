package utopia.genesis.shape.shape2D.transform

import utopia.genesis.animation.Animation
import utopia.genesis.animation.transform.{AnimatedAffineTransformable, AnimatedLinearTransformable}
import utopia.genesis.shape.shape2D.Matrix2D
import utopia.genesis.shape.shape3D.Matrix3D

/**
  * A combination trait of various transformable traits
  * @author Mikko Hilpinen
  * @since 26.12.2020, v2.4
  */
trait Transformable[+Transformed] extends LinearTransformable[Transformed] with AffineTransformable[Transformed]
	with AnimatedLinearTransformable[Animation[Transformed]] with AnimatedAffineTransformable[Animation[Transformed]]
{
	override def transformedWith(transformation: Animation[Matrix2D]) =
		transformation.map(transformedWith)
	
	override def affineTransformedWith(transformation: Animation[Matrix3D]) =
		transformation.map(transformedWith)
}
