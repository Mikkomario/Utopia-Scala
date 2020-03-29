package utopia.genesis.animation.animator

import utopia.genesis.animation.{Animation, TimedAnimation}
import utopia.genesis.image.{Image, Strip}
import utopia.genesis.shape.shape2D.{Point, Transformation}
import utopia.genesis.util.Drawer

/**
  * This sprite animator follows a set transformation animation
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
trait TransformingImageAnimator extends Animator[(Image, Point, Transformation)]
{
	// ABSTRACT	---------------------------
	
	/**
	  * @param progress Current animation progress
	  * @return Image and image origin for that progress state
	  */
	def imageAndOrigin(progress: Double): (Image, Point)
	/**
	  * @return Animation that determines transformation used at any given time
	  */
	def transformationAnimation: TimedAnimation[Transformation]
	
	
	// IMPLEMENTED	------------------------
	
	override def animationDuration = transformationAnimation.duration
	
	override protected def apply(progress: Double) =
	{
		val (img, origin) = imageAndOrigin(progress)
		(img, origin, transformationAnimation(progress))
	}
	
	override protected def draw(drawer: Drawer, item: (Image, Point, Transformation)) =
		item._1.drawWith(drawer.transformed(item._3), origin = item._2)
}

object TransformingImageAnimator
{
	// OTHER	---------------------------
	
	/**
	  * Creates a new animator that is based on a static image
	  * @param image A static image
	  * @param origin Image drawing origin
	  * @param transformation Transformation animation applied
	  * @return A new animator
	  */
	def apply(image: Image, origin: Point, transformation: TimedAnimation[Transformation]): TransformingImageAnimator =
		new TransformingStaticImageAnimator(image, origin, transformation)
	
	/**
	  * Creates a new animator that is based on a strip. The strip is completed in the same time as the transformation.
	  * @param strip A strip
	  * @param origin Image drawing origin (static among all strip images)
	  * @param transformation Transformation animation applied
	  * @return A new animator
	  */
	def apply(strip: Animation[Image], origin: Point, transformation: TimedAnimation[Transformation]): TransformingImageAnimator =
		new TransformingStripAnimator(strip, origin, transformation)
	
	/**
	  * Creates a new animator by wrapping two animations
	  * @param imageAnimation An image animation
	  * @param transformation A transformation animation
	  * @return A new animator that uses the specified animations
	  */
	def apply(imageAnimation: Animation[(Image, Point)], transformation: TimedAnimation[Transformation]): TransformingImageAnimator =
		new TransformingImageAnimatorWrapper(imageAnimation, transformation)
	
	
	// NESTED	---------------------------
	
	private class TransformingStaticImageAnimator(image: Image, origin: Point,
												  override val transformationAnimation: TimedAnimation[Transformation])
		extends TransformingImageAnimator
	{
		override def imageAndOrigin(progress: Double) = image -> origin
	}
	
	private class TransformingStripAnimator(strip: Animation[Image], origin: Point,
											override val transformationAnimation: TimedAnimation[Transformation])
		extends TransformingImageAnimator
	{
		override def imageAndOrigin(progress: Double) = strip(progress) -> origin
	}
	
	private class TransformingImageAnimatorWrapper(imageAnimation: Animation[(Image, Point)],
												   override val transformationAnimation: TimedAnimation[Transformation])
		extends TransformingImageAnimator
	{
		override def imageAndOrigin(progress: Double) = imageAnimation(progress)
	}
}