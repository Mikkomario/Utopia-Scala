package utopia.genesis.image

import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.genesis.graphics.Drawer
import utopia.paradigm.color.{Color, ColorShade}
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.HasSize

/**
  * A common trait for both mutable and immutable image representations.
  * Read only.
  * @author Mikko Hilpinen
  * @since 20.12.2024, v4.1.1
  */
trait ImageView extends HasSize
{
	// ABSTRACT	--------------------
	
	/**
	  * @return Alpha used when drawing this image
	  */
	def alpha: Double
	
	/**
	  * The bounds of this image when origin and size are both counted. The (0,0) coordinate is at the origin
	  * of this image.
	  */
	def bounds: Bounds
	/**
	  * @return The origin of this image that is relative to the image top left corner.
	  *         Note: These coordinates are relative to [[size]], and not necessarily image source resolution.
	  */
	def origin: Point
	/**
	  * @return Whether this image has a custom origin. False if the default origin (i.e. the top left corner) is used.
	  */
	def specifiesOrigin: Boolean
	
	/**
	  * @return Whether this image is empty (0x0)
	  */
	def isEmpty: Boolean
	
	/**
	  * @return An immutable image based on this image
	  */
	def toImage: Image
	/**
	  * @return A concrete image based on this image
	  */
	def toConcreteImage: ConcreteImage
	
	/**
	  * @return Maximum scaling, relative to this image's current state,
	  *         that may be applied without causing this image to get blurred
	  *         (i.e. without scaling it beyond its original size).
	  *
	  *         None if no maximum scaling may be calculated or applied.
	  */
	def maxScaling: Option[Double]
	
	/**
	  * @return Pixel data for this image.
	  */
	def pixels: Pixels
	/**
	  * @return The average shade of this image
	  */
	def shade: ColorShade
	
	/**
	  * @param point Targeted point in this image
	  * @param relativeToOrigin Whether the specified point is relative to the origin of this image,
	  *                         instead of being relative to the top left corner of this image (which is the default).
	  * @return A color of this image at the specified location
	  */
	def pixelAt(point: Point, relativeToOrigin: Boolean = false): Color
	/**
	  * @param area Targeted area within this image
	  * @param relativeToOrigin Whether the specified region is relative to the origin of this image,
	  *                         instead of being relative to the top left corner of this image (which is the default).
	  * @return Pixels within the targeted area
	  */
	def pixelsAt(area: Bounds, relativeToOrigin: Boolean = false): Pixels
	
	/**
	  * Draws this image using a specific drawer
	  * @param drawer A drawer
	  * @param position The position where this image's origin is drawn (default = (0, 0))
	  * @param transformation An additional linear transformation to apply (optional)
	  * @return Whether this image was fully drawn
	  */
	def drawWith(drawer: Drawer, position: Point = Point.origin, transformation: Option[Matrix2D] = None): Boolean
	/**
	  * Draws a region of this image using the specified drawer.
	  * @param drawer A drawer
	  * @param subRegion Region of this image that will be drawn, relative to the top left corner of this image.
	  * @param position The position where **this image's origin** is drawn (default = (0,0)).
	  * @return Whether the region was fully drawn already
	  */
	def drawSubImageWith(drawer: Drawer, subRegion: Bounds, position: Point = Point.origin): Boolean
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Whether this image contains some data
	  */
	def nonEmpty = !isEmpty
	
	/**
	  * Calculates the length of this image from the origin to the specified direction
	  * (E.g. if origin is at the center of this image, returns width or height halved)
	  * @param direction Direction towards which the distance is counted
	  * @return Starting from this image's origin, the length of this image towards that direction
	  */
	def lengthTowards(direction: Direction2D) = direction.sign match {
		case Positive => size(direction.axis) - origin(direction.axis)
		case Negative => origin(direction.axis)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param area Targeted area within this image
	  * @param relativeToOrigin Whether the specified point is relative to the origin of this image,
	  *                         instead of being relative to the top left corner of this image (which is the default).
	  * @return The average luminosity of the pixels in the targeted area
	  */
	def averageLuminosityOf(area: Bounds, relativeToOrigin: Boolean = false) =
		pixelsAt(area, relativeToOrigin).averageLuminosity
	/**
	  * @param area Targeted area within this image
	  * @param relativeToOrigin Whether the specified point is relative to the origin of this image,
	  *                         instead of being relative to the top left corner of this image (which is the default).
	  * @return The average relative luminance (perceived lightness) of the pixels in the targeted area
	  */
	def averageRelativeLuminanceOf(area: Bounds, relativeToOrigin: Boolean = false) =
		pixelsAt(area, relativeToOrigin).averageRelativeLuminance
}
