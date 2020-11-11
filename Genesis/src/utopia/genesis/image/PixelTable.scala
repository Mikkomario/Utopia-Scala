package utopia.genesis.image

import java.awt.image.BufferedImage

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.{Area2D, Bounds, Point, Size}

object PixelTable
{
	/**
	 * An empty pixel table
	 */
	val empty = PixelTable(Vector())
	
	/**
	  * Converts a buffered image to a pixel table
	  * @param image The source image
	  * @return A pixel table based on the image
	  */
	def fromBufferedImage(image: BufferedImage) =
	{
		val w = image.getWidth
		val h = image.getHeight
		
		val rawPixels = new Array[Int](w * h)
		image.getRGB(0, 0, w, h, rawPixels, 0, w)
		
		val pixelVector = rawPixels.toVector.map(Color.fromInt)
		
		// color = array[y * scansize + x]
		PixelTable((0 until h).map { y => (0 until w).map { x => pixelVector(y * w + x) }.toVector }.toVector)
	}
}

/**
  * This class represents a 2d tile of pixels
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1+
  */
// First y-coordinate, then x-coordinate
case class PixelTable private(_pixels: Vector[Vector[Color]])
{
	// COMPUTED	-----------------------
	
	/**
	  * @return Whether this pixel table is completely empty
	  */
	def isEmpty = _pixels.headOption.forall { _.isEmpty }
	
	/**
	  * @return The width of this table in pixels
	  */
	def width = if (_pixels.isEmpty) 0 else _pixels.head.size
	
	/**
	  * @return The height of this table in pixels
	  */
	def height = _pixels.size
	
	/**
	  * @return The size of this table in pixels
	  */
	def size = Size(width, height)
	
	/**
	  * @return A vector containing each pixel color value
	  */
	private def toVector = _pixels.flatten
	
	/**
	  * @return A vector containing each pixel color rgb value
	  */
	private def toRGBVector = toVector.map { _.toInt }
	
	/**
	  * @return A buffered image based on this pixel data
	 *  @throws IllegalArgumentException If this pixel table is empty
	  */
	def toBufferedImage =
	{
		val newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
		writeToImageNoCheck(newImage, Point.origin)
		newImage
	}
	
	/**
	  * @return A copy of this pixel table where x-axis is reversed
	  */
	def flippedHorizontally = PixelTable(_pixels.map { _.reverse })
	
	/**
	  * @return A copy of this pixel table where y-axis is reversed
	  */
	def flippedVertically = PixelTable(_pixels.reverse)
	
	
	// OPERATORS	-------------------
	
	/**
	  * Finds a pixel from this table
	  * @param x Pixel x-coordinate
	  * @param y Pixel y-coordinate
	  * @return Pixel color
	  * @throws IndexOutOfBoundsException If x or y are out of bounds
	  */
	def apply(x: Int, y: Int) = _pixels(y)(x)
	
	/**
	  * Finds a pixel from this table
	  * @param point the relative pixel position
	  * @return Pixel color
	  * @throws IndexOutOfBoundsException If point is not within the bounds of this table
	  */
	def apply(point: Point): Color = apply(point.x.toInt, point.y.toInt)
	
	/**
	  * @param bounds Target area within this pixel table (doesn't have to be contained within this table's area)
	  * @return An iterator of the pixels which overlap with the specified area
	  */
	def apply(bounds: Bounds): Iterator[Color] =
	{
		if (isEmpty)
			Iterator.empty
		else
			bounds.within(Bounds(Point.origin, size)) match
			{
				case Some(area) => new PixelIterator(area.y.toInt, area.bottomY.toInt, area.x.toInt, area.rightX.toInt)
				case None => Iterator.empty
			}
	}
	
	
	// OTHER	-----------------------
	
	/**
	  * Finds the color value of a single pixel in this table
	  * @param point Targeted point in this table
	  * @return The color of the pixel at that location. None if this table doesn't contain such a location.
	  */
	def lookup(point: Point) =
	{
		val y = point.y.toInt
		if (y >= 0 && y < _pixels.size)
		{
			val row = _pixels(y)
			val x = point.x.toInt
			if (x >= 0 && x < row.size)
				Some(row(x))
			else
				None
		}
		else
			None
	}
	
	/**
	  * @param area Targeted area in this table
	  * @return The average color value inside the area
	  */
	def averageOf(area: Bounds) = Color.average(apply(area).groupMap(200)(Color.average))
	
	/**
	  * @param area Targeted area in this table
	  * @return The average color luminosity inside the area
	  */
	def averageLuminosityOf(area: Bounds) =
	{
		apply(area).map { c => (c.luminosity * c.alpha) -> c.alpha }
			.reduceOption { (a, b) => (a._1 + b._1) -> (a._2 + b._2) } match
		{
			case Some((totalLuminosity, totalAlpha)) => totalLuminosity / totalAlpha
			case None => 0.0
		}
	}
	
	/**
	  * Takes a portion of this table that is contained within the target area
	  * @param area Clipping area
	  * @return A copy of this table that only contains the part within the specified area
	  */
	def clippedTo(area: Bounds) = PixelTable(_pixels.slice(area.y.toInt, area.bottomY.toInt).map { row =>
		row.slice(area.x.toInt, area.rightX.toInt) })
	
	/**
	  * Maps each pixel color
	  * @param f A color mapping function
	  * @return A mapped pixel table
	  */
	def map(f: Color => Color) = PixelTable(_pixels.map { _.map(f) })
	
	/**
	  * Maps each pixel color, also providing pixel coordinates for the mapping function
	  * @param f A color mapping function that also takes pixel coordinates
	  * @return A mapped pixel table
	  */
	def mapWithIndex(f: (Color, Point) => Color) = PixelTable(_pixels.indices.map
	{
		y =>
		{
			val column = _pixels(y)
			column.indices.map { x => f(column(x), Point(x, y)) }.toVector
		}
		
	}.toVector)
	
	/**
	  * Maps the pixels within a specific relative area
	  * @param area A relative area that determines which pixels will be mapped
	  * @param f A color mapping function
	  * @return A (partially) mapped pixel table
	  */
	def mapArea(area: Area2D)(f: Color => Color) = mapWithIndex { (c, p) => if (area.contains(p)) f(c) else c }
	
	/**
	  * Writes this pixel table to an image
	  * @param image The target image
	  * @param topLeft The coordinate of the top-left corner of this table in the target image (default = (0, 0))
	  */
	def writeToImage(image: BufferedImage, topLeft: Point = Point.origin) =
	{
		val imageBounds = Bounds(Point.origin, Size(image.getWidth, image.getHeight))
		val writeBounds = Bounds(topLeft, size)
		
		// Default case: Written area is within image area
		if (imageBounds.contains(writeBounds))
			writeToImageNoCheck(image, topLeft)
		// If written area only partially overlaps with image area, has to crop this table first
		else if (imageBounds.collisionMtvWith(writeBounds).isDefined)
			clippedTo(imageBounds - topLeft).writeToImageNoCheck(image, topLeft)
	}
	
	private def writeToImageNoCheck(image: BufferedImage, topLeft: Point) = image.setRGB(topLeft.x.toInt, topLeft.y.toInt,
		width, height, toRGBVector.toArray, 0, width)
	
	
	// NESTED	-------------------------------
	
	private class PixelIterator(yStart: Int, yEnd: Int, xStart: Int, xEnd: Int) extends Iterator[Color]
	{
		// ATTRIBUTES	-----------------------
		
		private var currentY = yStart
		private var rowIterator = _pixels(currentY).slice(xStart, xEnd).iterator
		
		
		// IMPLEMENTED	-----------------------
		
		override def hasNext = rowIterator.hasNext && currentY < yEnd
		
		override def next() =
		{
			rowIterator.nextOption() match
			{
				// Case: Still row left to scan
				case Some(rowItem) => rowItem
				// Case: New row needs to be started
				case None =>
					currentY += 1
					rowIterator = _pixels(currentY).slice(xStart, xEnd).iterator
					rowIterator.next()
			}
		}
	}
}
