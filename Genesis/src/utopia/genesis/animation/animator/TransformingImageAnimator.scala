package utopia.genesis.animation.animator

import utopia.paradigm.animation.{Animation, TimedAnimation}
import utopia.genesis.graphics.Drawer
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape3d.Matrix3D

object TransformingImageAnimator
{
	// OTHER	---------------------------
	
	/**
	  * Creates a new animator that is based on a static image
	  * @param image A static image
	  * @param transformation Transformation animation applied
	  * @return A new animator
	  */
	def apply(image: Image, transformation: TimedAnimation[Matrix3D]): TransformingImageAnimator =
		new TransformingStaticImageAnimator(image, transformation)
	
	/**
	  * Creates a new animator that is based on a strip. The strip is completed in the same time as the transformation.
	  * @param strip A strip
	  * @param transformation Transformation animation applied
	  * @return A new animator
	  */
	def apply(strip: Animation[Image], transformation: TimedAnimation[Matrix3D]): TransformingImageAnimator =
		new TransformingStripAnimator(strip, transformation)
	
	
	// NESTED	---------------------------
	
	private class TransformingStaticImageAnimator(image: Image,
	                                              override val transformationAnimation: TimedAnimation[Matrix3D])
		extends TransformingImageAnimator
	{
		override def image(progress: Double) = image
	}
	
	private class TransformingStripAnimator(strip: Animation[Image],
	                                        override val transformationAnimation: TimedAnimation[Matrix3D])
		extends TransformingImageAnimator
	{
		override def image(progress: Double) = strip(progress)
	}
}

/**
  * This sprite animator follows a set transformation animation
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
abstract class TransformingImageAnimator extends Animator[(Image, Matrix3D)]
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
	def transformationAnimation: TimedAnimation[Matrix3D]
	
	
	// IMPLEMENTED	------------------------
	
	override def animationDuration = transformationAnimation.duration
	
	override protected def apply(progress: Double) =
		image(progress) -> transformationAnimation(progress)
	
	override protected def draw(drawer: Drawer, item: (Image, Matrix3D)) =
		item._1.drawWith(drawer * item._2)
}