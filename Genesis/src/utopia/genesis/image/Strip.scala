package utopia.genesis.image

import utopia.genesis.animation.Animation
import utopia.genesis.shape.shape2D.{Point, Size}
import utopia.genesis.util.Fps

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
	  * Performs a mapping function on the images in this strip
	  * @param f A mapping function
	  * @return A mapped copy of this strip
	  */
	def map(f: Image => Image) = copy(images = images.map(f))
	
	/**
	  * Performs a mapping function on the images in this strip
	  * @param f A mapping function. May return multiple images.
	  * @return A mapped copy of this strip
	  */
	def flatMap(f: Image => IterableOnce[Image]) = copy(images = images.flatMap(f))
	
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
	def toTimedAnimation(fps: Fps) = over(images.size * fps.interval)
	
	/**
	  * @param getImageOrigin A function for calculating an image origin. Default = origin at image center.
	  * @return An animation that returns both image and origin
	  */
	def toAnimationWithOrigin(getImageOrigin: Image => Point = _.size.toPoint / 2) =
		map { i => i -> getImageOrigin(i) }
	
	/**
	  * @param fps Animation speed in frames per second
	  * @param getImageOrigin A function for calculating an image origin. Default = origin at image center.
	  * @return A timed animation that returns both image and origin
	  */
	def toTimedAnimationWithOrigin(fps: Fps, getImageOrigin: Image => Point = _.size.toPoint / 2) =
		toTimedAnimation(fps).map { i => i -> getImageOrigin(i) }
}
