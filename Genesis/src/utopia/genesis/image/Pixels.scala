package utopia.genesis.image

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Matrix, MatrixLike, Pair}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.color.{Color, ColorShade}
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

import java.awt.image.BufferedImage
import scala.collection.IndexedSeqView

object Pixels
{
	/**
	  * An empty pixel table
	  */
	val empty = apply(Matrix.empty)
	
	/**
	  * Converts a buffered image to a pixel table
	  * @param image The source image
	  * @param lazily Whether the image data should be read lazily (default = false)
	  * @return A pixel table based on the image
	  */
	def fromBufferedImage(image: BufferedImage, lazily: Boolean = false) = {
		val w = image.getWidth
		val h = image.getHeight
		
		// Case: Initializes the pixels lazily, one by one
		if (lazily)
			apply(Matrix.lazyFill(w, h) { p => Color.fromInt(image.getRGB(p.first, p.second)) })
		// Case: Initializes the images all at once
		else {
			val rawPixels = new Array[Int](w * h)
			image.getRGB(0, 0, w, h, rawPixels, 0, w)
			apply(Matrix(Vector.from(rawPixels.view.map(Color.fromInt)), w, h, rowsToColumns = true))
		}
	}
}

/**
  * This class represents a 2d tile of pixels
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1+
  */
case class Pixels(private val matrix: Matrix[Color]) extends MatrixLike[Color, Matrix, Pixels] with Matrix[Color]
{
	// ATTRIBUTES   -------------------
	
	/**
	  * The average color of these pixels
	  */
	lazy val average = Color.average(matrix.iterator.groupMap(200)(Color.average))
	/**
	  * @return The average luminosity of the pixels in this table
	  */
	lazy val averageLuminosity = Color.averageLuminosityOf(matrix.iterator)
	/**
	  * @return The average relative luminance (perceived lightness) of the pixels in this table
	  */
	lazy val averageRelativeLuminance = Color.averageRelativeLuminanceOf(matrix.iterator)
	
	override lazy protected val sizeView: Pair[View[Int]] = Pair(Lazy { width }, Lazy { height })
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return The size of this table in pixels
	  */
	def area = Size(width, height)
	
	/**
	  * @return The average shade of these pixels
	  */
	def averageShade = ColorShade.forLuminosity(averageRelativeLuminance)
	
	/**
	  * @return A vector containing each pixel color rgb value
	  */
	private def rgbIterator = matrix.iteratorByRows.map { _.toInt }
	
	/**
	  * @return A buffered image based on this pixel data
	 *  @throws IllegalArgumentException If this pixel table is empty
	  */
	def toBufferedImage = {
		val newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
		writeToImageNoCheck(newImage, Point.origin)
		newImage
	}
	
	/**
	  * @return A copy of this pixel table where x-axis is reversed
	  */
	def flippedHorizontally = copy(matrix.mapRows { _.reverse })
	/**
	  * @return A copy of this pixel table where y-axis is reversed
	  */
	def flippedVertically = copy(matrix.mapColumns { _.reverse })
	
	
	// IMPLEMENTED	-------------------
	
	override def width: Int = matrix.width
	override def height: Int = matrix.height
	
	override def columnsView: IndexedSeqView[IndexedSeqView[Color]] = matrix.columnsView
	override def rowsView: IndexedSeqView[IndexedSeqView[Color]] = matrix.rowsView
	
	override def columns = matrix.columns
	override def rows = matrix.rows
	
	override def transpose: Pixels = copy(matrix.transpose)
	
	override def self: Pixels = this
	protected override def empty = Pixels.empty
	
	override def view(area: Pair[NumericSpan[Int]]) = copy(matrix.view(area))
	
	override def map[B](f: Color => B): Matrix[B] = matrix.map(f)
	override def mapWithIndex[B](f: (Color, Pair[Int]) => B): Matrix[B] = matrix.mapWithIndex(f)
	
	override def lazyMap[B](f: Color => B): Matrix[B] = matrix.lazyMap(f)
	override def lazyMapWithIndex[B](f: (Color, Pair[Int]) => B): Matrix[B] = matrix.lazyMapWithIndex(f)
	
	
	// OTHER	-----------------------
	
	/**
	  * Finds a pixel from this table
	  * @param point the relative pixel position
	  * @return Pixel color
	  * @throws IndexOutOfBoundsException If point is not within the bounds of this table
	  */
	def apply(point: Point): Color = matrix(point.xyPair.map { _.toInt })
	
	/**
	  * Finds the color value of a single pixel in this table
	  * @param point Targeted point in this table
	  * @return The color of the pixel at that location. None if this table doesn't contain such a location.
	  */
	def lookup(point: Point) = matrix.lift(point.xyPair.map { _.toInt })
	
	/**
	  * @param bounds Target area within this pixel table (doesn't have to be contained within this table's area)
	  * @return A sub-region of this pixel-set that overlaps with the specified area
	  */
	def view(bounds: Bounds): Pixels =
		copy(matrix.view(bounds.xyPair.map { span => NumericSpan(span.start.toInt, span.end.toInt) }))
	
	/**
	  * Maps each pixel color
	  * @param f A color mapping function
	  * @return A mapped pixel table
	  */
	def map(f: Color => Color) = copy(matrix.map(f))
	/**
	  * Maps each pixel color, also providing pixel coordinates for the mapping function
	  * @param f A color mapping function that also takes pixel coordinates
	  * @return A mapped pixel table
	  */
	def mapWithIndex(f: (Color, Pair[Int]) => Color) = copy(matrix.mapWithIndex(f))
	/**
	  * Maps each pixel color, also providing pixel coordinates for the mapping function
	  * @param f A color mapping function that also takes pixel coordinates
	  * @return A mapped pixel table
	  */
	def mapPoints(f: (Color, Point) => Color) =
		mapWithIndex { (color, pos) => f(color, Point.from(pos.map { _.toDouble })) }
	/**
	  * Maps the pixels within a specific relative area
	  * @param area A relative area that determines which pixels will be mapped
	  * @param f A color mapping function
	  * @return A (partially) mapped pixel table
	  */
	def mapArea(area: Area2D)(f: Color => Color) = mapPoints { (c, p) => if (area.contains(p)) f(c) else c }
	def lazyMap(f: Color => Color) = copy(matrix.lazyMap(f))
	def lazyMapWithIndex(f: (Color, Pair[Int]) => Color) = copy(matrix.lazyMapWithIndex(f))
	
	/**
	  * Writes this pixel table to an image
	  * @param image The target image
	  * @param topLeft The coordinate of the top-left corner of this table in the target image (default = (0, 0))
	  */
	def writeToImage(image: BufferedImage, topLeft: Point = Point.origin) = {
		val imageBounds = Bounds(Point.origin, Size(image.getWidth, image.getHeight))
		val writeBounds = Bounds(topLeft, area)
		
		// Default case: Written area is within image area
		if (imageBounds.contains(writeBounds))
			writeToImageNoCheck(image, topLeft)
		// If written area only partially overlaps with image area, has to crop this table first
		else if (imageBounds.collisionMtvWith(writeBounds).isDefined)
			view(imageBounds - topLeft).writeToImageNoCheck(image, topLeft)
	}
	
	private def writeToImageNoCheck(image: BufferedImage, topLeft: Point) =
		image.setRGB(topLeft.x.toInt, topLeft.y.toInt, width, height, rgbIterator.toArray, 0, width)
}
