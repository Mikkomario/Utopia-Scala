package utopia.genesis.image

import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point

import java.awt.image.BufferedImage

object Image extends ImageFactory[ConcreteImage]
{
	override def empty = ConcreteImage.empty
	
	override def apply(image: BufferedImage, scaling: Vector2D, alpha: Double, origin: Option[Point]) =
		ConcreteImage(image, scaling, alpha, origin)
	
	override def fromPixels(pixels: Pixels) = ConcreteImage.fromPixels(pixels)
}

/**
  * This is a wrapper for the buffered image class
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1
  */
trait Image extends ImageLike[Image]