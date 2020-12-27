package utopia.genesis.image

import java.awt.image.BufferedImage
import utopia.genesis.color.Color
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape2D.transform.AffineTransformation
import utopia.genesis.shape.shape2D.{Bounds, Direction2D, Matrix2D, Point, Size, Vector2D}
import utopia.genesis.util.Drawer

/**
  * A common trait for image implementations
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2.4
  */
trait ImageLike
{
	// ABSTRACT	--------------------
	
	/**
	  * @return Wrapped mutable awt image
	  */
	protected def source: Option[BufferedImage]
	
	/**
	  * @return Scaling applied to this image
	  */
	def scaling: Vector2D
	
	/**
	  * @return Alpha used when drawing this image
	  */
	def alpha: Double
	
	/**
	  * @return Measurements of the original image data
	  */
	def sourceResolution: Size
	
	/**
	  * @return Scaled / applied size of this image
	  */
	def size: Size
	
	/**
	  * The bounds of this image when origin and size are both counted. The (0,0) coordinate is at the origin
	  * of this image.
	  */
	def bounds: Bounds
	
	/**
	  * @return A specifically set origin for this image. None if origin is unspecified.
	  */
	def specifiedOrigin: Option[Point]
	
	/**
	  * @return Whether this image is empty (0x0)
	  */
	def isEmpty: Boolean
	
	/**
	  * @return Pixel data for this image. The acquisition of these pixels may be a slow operation.
	  */
	def pixels: PixelTable
	
	/**
	  * @return Precalculated pixel data for this image. This should return faster than <i>pixels</i>, but returns
	  *         None if the pixels haven't been precalculated.
	  */
	def preCalculatedPixels: Option[PixelTable]
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Whether this image contains some data
	  */
	def nonEmpty = !isEmpty
	
	/**
	  * @return The width of this image in pixels
	  */
	def width = size.width
	
	/**
	  * @return The height of this image in pixels
	  */
	def height = size.height
	
	/**
	  * @return Whether this image has a specified origin
	  */
	def specifiesOrigin = specifiedOrigin.isDefined
	
	/**
	  * @return The origin of this image that is relative to this image's source resolution and not necessarily
	  *         the current size.
	  */
	def sourceResolutionOrigin = specifiedOrigin.getOrElse(Point.origin)
	
	/**
	  * @return The origin of this image that is relative to the image size and not source resolution
	  */
	def origin = specifiedOrigin.map { _ * scaling }.getOrElse(Point.origin)
	
	/**
	  * Calculates the length of this image from the origin to the specified direction (Eg. if origin is at
	  * the center of this image, returns width or height halved)
	  * @param direction Direction towards which the distance is counted
	  * @return Starting from this image's origin, the length of this image towards that direction
	  */
	def lengthTowards(direction: Direction2D) = direction.sign match
	{
		case Positive => size.along(direction.axis) - origin.along(direction.axis)
		case Negative => origin.along(direction.axis)
	}
	
	
	// IMPLEMENTED	----------------
	
	override def toString =
	{
		val alphaPortion = if (alpha == 1) "" else s" ${(alpha * 100).toInt}% Alpha"
		val originPortion = specifiedOrigin match
		{
			case Some(origin) => s" Origin at ${origin * scaling}"
			case None => ""
		}
		s"Image ($size$alphaPortion$originPortion)"
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param point Targeted point in this image <b>relative to this image's origin</b>
	  * @return A color of this image at the specified location
	  */
	def pixelAt(point: Point) =
	{
		// Utilizes a pre-calculated pixel table if one is already available,
		// although will not create one just for this method call
		preCalculatedPixels match
		{
			case Some(pixels) => pixels.lookup(point).getOrElse(Color.transparentBlack)
			case None =>
				source match
				{
					case Some(image) =>
						// Converts the point to an image coordinate
						val pointInImage = ((point - bounds.topLeft) / scaling).round
						val x = pointInImage.x.toInt
						val y = pointInImage.y.toInt
						if (x >= 0 && y >= 0 && x < image.getWidth && y < image.getHeight)
						{
							// Fetches the pixel color in that location
							val rgb = image.getRGB(x, y)
							Color.fromInt(rgb)
						}
						else
							Color.transparentBlack
					case None => Color.transparentBlack
				}
		}
	}
	
	/**
	  * @param area Targeted area within this image. The (0,0) location is relative to the top left corner of this image
	  * @return An iterator that traverses through the pixels in that area
	  */
	def pixelsAt(area: Bounds) =
	{
		// Uses a pixel table if one is available
		preCalculatedPixels match
		{
			case Some(pixels) => pixels(area / scaling)
			case None =>
				(area / scaling).within(Bounds(Point.origin, sourceResolution)) match
				{
					case Some(insideArea) =>
						if (insideArea.size.isPositive)
						{
							// If the specified area covers 50% of this image or more, calculates the whole pixel table
							if (insideArea.area >= sourceResolution.area * 0.5)
								pixels(insideArea)
							else
							{
								source match
								{
									// Iterates over the targeted pixels
									case Some(image) =>
										new ImageIterator(image, insideArea.x.toInt, insideArea.y.toInt,
											insideArea.rightX.toInt, insideArea.bottomY.toInt)
									case None => Vector().iterator
								}
							}
						}
						else
							Vector().iterator
					case None => Vector().iterator
				}
		}
	}
	
	/**
	  * @param area Targeted area within this image. The (0,0) is at the top left corner of this image
	  * @return The average luminosity of the pixels in the targeted area
	  */
	def averageLuminosityOf(area: Bounds) = Color.averageLuminosityOf(pixelsAt(area))
	
	/**
	  * Draws this image using a specific drawer
	  * @param drawer A drawer
	  * @param position The position where this image's origin is drawn (default = (0, 0))
	  * @return Whether this image was fully drawn
	  */
		// FIXME: These transformations don't work at this time
	def drawWith(drawer: Drawer, position: Point = Point.origin, transformation: Option[Matrix2D] = None) =
	{
		source.forall { s =>
			// Translates the drawer so that the desired image origin lies at (0,0)
			// Then applies scaling and other transformation(s)
			// TODO: Skip unnecessary transformations
			val baseTransform = AffineTransformation(position.toVector).scaled(scaling) //Matrix2D.scaling(scaling).translated(position.toVector)
			val transformedDrawer = transformation match
			{
				case Some(t) => drawer * baseTransform * t
				case None => drawer * baseTransform
			}
			// Finally draws the image to -origin location so that the image origin will overlap with the desired
			// position
			if (alpha == 1.0)
				transformedDrawer.drawImage(s, -origin)
			else
				transformedDrawer.withAlpha(alpha).drawImage(s, -origin)
		}
	}
}

private class ImageIterator(image: BufferedImage, startX: Int, startY: Int, endX: Int, endY: Int)
	extends Iterator[Color]
{
	// ATTRIBUTES	-----------------------
	
	private val lastX = endX - 1
	
	private var nextX = startX
	private var nextY = startY
	
	
	// IMPLEMENTED	-----------------------
	
	override def hasNext = nextY < endY && nextX < endX
	
	override def next() =
	{
		// Fetches the color
		val rgb = image.getRGB(nextX, nextY)
		// Moves the cursor
		if (nextX < lastX)
			nextX += 1
		else
		{
			nextX = startX
			nextY += 1
		}
		Color.fromInt(rgb)
	}
}
