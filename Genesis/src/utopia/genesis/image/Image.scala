package utopia.genesis.image

import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point

import java.awt.image.BufferedImage

object Image extends ImageFactory[ConcreteImage]
{
	// IMPLEMENTED  ------------------------
	
	override def empty = ConcreteImage.empty
	
	override def apply(image: BufferedImage, scaling: Vector2D, alpha: Double, origin: Option[Point]) =
		ConcreteImage(image, scaling, alpha, origin)
	
	override def fromPixels(pixels: Pixels) = ConcreteImage.fromPixels(pixels)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param variants Different sized versions of a single image
	  * @return An image that dynamically switches between displayed versions, based on display size
	  */
	def scaleByCombining(variants: Seq[Image]) = variants.emptyOneOrMany match {
		case None => empty
		case Some(Left(only)) => only
		case Some(Right(many)) => CompositeScalingImage(many)
	}
}

/**
  * This is a wrapper for the buffered image class
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1
  */
trait Image extends ImageLike[Image]