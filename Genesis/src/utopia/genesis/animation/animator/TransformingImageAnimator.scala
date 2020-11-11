package utopia.genesis.animation.animator

import utopia.genesis.animation.{Animation, TimedAnimation}
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Transformation
import utopia.genesis.util.Drawer

/**
  * This sprite animator follows a set transformation animation
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
trait TransformingImageAnimator extends Animator[(Image, Transformation)]
{
	// ABSTRACT	---------------------------
	
	/**
	  * @param progress Current animation progress
	  * @return Image for that progress state
	  */
	def image(progress: Double): Image
	/**
	  * @return Animation that determines transformation used at any given time
	  */
	def transformationAnimation: TimedAnimation[Transformation]
	
	
	// IMPLEMENTED	------------------------
	
	override def animationDuration = transformationAnimation.duration
	
	override protected def apply(progress: Double) =
		image(progress) -> transformationAnimation(progress)
	
	override protected def draw(drawer: Drawer, item: (Image, Transformation)) =
		item._1.drawWith(drawer.transformed(item._2))
}

object TransformingImageAnimator
{
	// OTHER	---------------------------
	
	/**
	  * Creates a new animator that is based on a static image
	  * @param image A static image
	  * @param transformation Transformation animation applied
	  * @return A new animator
	  */
	def apply(image: Image, transformation: TimedAnimation[Transformation]): TransformingImageAnimator =
		new TransformingStaticImageAnimator(image, transformation)
	
	/**
	  * Creates a new animator that is based on a strip. The strip is completed in the same time as the transformation.
	  * @param strip A strip
	  * @param transformation Transformation animation applied
	  * @return A new animator
	  */
	def apply(strip: Animation[Image], transformation: TimedAnimation[Transformation]): TransformingImageAnimator =
		new TransformingStripAnimator(strip, transformation)
	
	
	// NESTED	---------------------------
	
	private class TransformingStaticImageAnimator(image: Image,
												  override val transformationAnimation: TimedAnimation[Transformation])
		extends TransformingImageAnimator
	{
		override def image(progress: Double) = image
	}
	
	private class TransformingStripAnimator(strip: Animation[Image],
											override val transformationAnimation: TimedAnimation[Transformation])
		extends TransformingImageAnimator
	{
		override def image(progress: Double) = strip(progress)
	}
}