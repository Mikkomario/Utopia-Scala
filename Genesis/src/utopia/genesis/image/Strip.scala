package utopia.genesis.image

import utopia.flow.util.Mutate
import utopia.paradigm.animation.Animation
import utopia.genesis.util.Fps
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * A strip consists of multiple sequential images
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1+
  */
case class Strip(images: Seq[ConcreteImage]) extends Animation[Image]
{
	// ATTRIBUTES	-------------------
	
	/**
	  * The bounds which contain all the images in this strip
	  */
	lazy val bounds = if (images.isEmpty) Bounds.zero else Bounds.around(images.map { _.bounds })
	
	/**
	  * The width of a single image in this strip
	  */
	private lazy val imageWidth = images.foldLeft(0.0) { _ max _.width }
	/**
	  * The height of a single image in this strip
	  */
	private lazy val imageHeight = images.foldLeft(0.0) { _ max _.height }
	/**
	  * The size of a single image in this strip
	  */
	private lazy val imageSize = Size(imageWidth, imageHeight)
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return The position where the (0,0) drawing coordinates should be placed in order to fit all images in this
	  *         strip to positive vector space (=> no image will be drawn to the above or left area of (0,0))
	  */
	def drawPosition = -bounds.position
	
	/**
	  * @return The size of the area this stip uses when drawing all images
	  */
	def size = bounds.size
	
	/**
	  * @return The width of the area this stip uses when drawing all images
	  */
	def width = bounds.width
	
	/**
	  * @return The height of the area this stip uses when drawing all images
	  */
	def height = bounds.height
	
	/**
	  * @return The length (number of images) of this strip
	  */
	def length = images.size
	
	/**
	  * @return A copy of this strip where the order of images is reversed
	  */
	def reverse = Strip(images.reverse)
	
	/**
	  * @return A copy of this strip with origin consistently placed at the center or each image
	  */
	def withCenterOrigin = Strip(images.map { _.withCenterOrigin })
	
	/**
	  * @return A copy of this strip where the origin is placed at the top left corner but the images are placed
	  *         so that they share the same center coordinate
	  */
	def withTopLeftOriginAndCentersAligned = Strip(images.map { image =>
		image.withOrigin((image.size - imageSize).toPoint / 2 ) })
	
	
	// IMPLEMENTED	-------------------
	
	override def apply(progress: Double): Image = apply((length * progress).toInt)
	
	
	// OTHER	-----------------------
	
	/**
	  * Performs a mapping function on the images in this strip
	  * @param f A mapping function
	  * @return A mapped copy of this strip
	  */
	def map(f: Mutate[ConcreteImage]) = copy(images = images.map(f))
	
	/**
	  * Performs a mapping function on the images in this strip
	  * @param f A mapping function. May return multiple images.
	  * @return A mapped copy of this strip
	  */
	def flatMap(f: Image => IterableOnce[ConcreteImage]) = copy(images = images.flatMap(f))
	
	/**
	  * Finds a specific image from this strip
	  * @param index The image index (0 is the first image)
	  * @return An image from the specified index
	  */
	def apply(index: Int) = {
		if (index >= 0)
			images(index % length)
		else
			images(length + (index % length))
	}
	
	/**
	  * @param sourceOrigin A new image origin, relative to the image source resolution
	  * @return A copy of this strip where all images have the specified source resolution origin
	  */
	def withSourceResolutionOrigin(sourceOrigin: Point) = Strip(images.map { _.withSourceResolutionOrigin(sourceOrigin) })
	
	/**
	  * @param origin A new image origin, relative to the image size
	  * @return A copy of this strip where all images have the specified origin
	  */
	def withOrigin(origin: Point) = Strip(images.map { _.withOrigin(origin) })
	
	/**
	  * @param fps Animation speed in frames per second
	  * @return An animation based on this strip with specified animation speed
	  */
	def toTimedAnimation(fps: Fps) = over(images.size * fps.interval)
}
