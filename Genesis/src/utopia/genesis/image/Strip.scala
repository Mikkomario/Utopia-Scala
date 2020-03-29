package utopia.genesis.image

import utopia.genesis.animation.Animation
import utopia.genesis.shape.shape2D.Size
import utopia.genesis.util.FPS

/**
  * A strip consists of multiple sequential images
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1+
  */
case class Strip(images: Vector[Image]) extends Animation[Image]
{
	// ATTRIBUTES	-------------------
	
	/**
	  * The width of a single image in this strip
	  */
	lazy val imageWidth = images.foldLeft(0.0) { _ max _.width }
	/**
	  * The height of a single image in this strip
	  */
	lazy val imageHeight = images.foldLeft(0.0) { _ max _.height }
	/**
	  * The size of a single image in this strip
	  */
	lazy val imageSize = Size(imageWidth, imageHeight)
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return The length (number of images) of this strip
	  */
	def length = images.size
	
	/**
	  * @return A copy of this strip where the order of images is reversed
	  */
	def reverse = Strip(images.reverse)
	
	
	// IMPLEMENTED	-------------------
	
	override def apply(progress: Double): Image = apply((length * progress).toInt)
	
	
	// OTHER	-----------------------
	
	/**
	  * Finds a specific image from this strip
	  * @param index The image index (0 is the first image)
	  * @return An image from the specified index
	  */
	def apply(index: Int) =
	{
		if (index >= 0)
			images(index % length)
		else
			images(length + (index % length))
	}
	
	/**
	  * @param fps Animation speed in frames per second
	  * @return An animation based on this strip with specified animation speed
	  */
	def toTimedAnimation(fps: FPS) = over(images.size * fps.interval)
}
