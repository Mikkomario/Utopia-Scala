package utopia.genesis.image

import java.awt.image.{BufferedImage, BufferedImageOp}
import java.io.FileNotFoundException
import java.nio.file.{Files, Path}

import utopia.flow.util.AutoClose._
import utopia.flow.util.NullSafe._
import javax.imageio.ImageIO
import utopia.flow.datastructure.mutable.Lazy
import utopia.genesis.color.Color
import utopia.genesis.image.transform.{Blur, HueAdjust, IncreaseContrast, Invert, Sharpen, Threshold}
import utopia.genesis.shape.{Angle, Rotation, Vector3D, VectorLike}
import utopia.genesis.shape.shape2D.{Area2D, Bounds, Point, Size, Transformation}
import utopia.genesis.util.{Drawer, Scalable}

import scala.util.{Failure, Success, Try}

object Image
{
	/**
	 * A zero sized image with no pixel data
	 */
	val empty = new Image(None, Vector3D.identity, 1.0, new Lazy(() => PixelTable.empty))
	
	/**
	  * Creates a new image
	  * @param image The original buffered image source
	  * @param scaling The scaling applied to the image
	  * @param alpha The maximum alpha value used when drawing this image [0, 1] (default = 1 = fully visible)
	  * @return A new image
	  */
	def apply(image: BufferedImage, scaling: Vector3D = Vector3D.identity, alpha: Double = 1.0): Image = Image(Some(image),
		scaling, alpha, Lazy(PixelTable.fromBufferedImage(image)))
	
	/**
	  * Reads an image from a file
	  * @param path The path the image is read from
	  * @param readClass Class through which the resource is read from.
	  *                  Leave to None when reading files outside program resources. (Default = None)
	  * @return The read image wrapped in Try
	  */
	def readFrom(path: Path, readClass: Option[Class[_]] = None) =
	{
		// Checks that file exists (not available with class read method)
		if (readClass.isDefined || Files.exists(path))
		{
			// ImageIO and class may return null. Image is read through class, if one  is provided
			val readResult = Try { readClass.map { c => c.getResourceAsStream("/" + path.toString).toOption
				.flatMap { _.consume { stream => ImageIO.read(stream).toOption } } }
				.getOrElse { ImageIO.read(path.toFile).toOption } }
			
			readResult.flatMap
			{
				case Some(result) => Success(apply(result))
				case None => Failure(new NoImageReaderAvailableException("Cannot read image from file: " + path.toString))
			}
		}
		else
			Failure(new FileNotFoundException(s"No (image) file at: ${Try{ path.toAbsolutePath }.getOrElse(path) }"))
	}
	
	/**
	 * Reads an image from a file. If image is not available, returns an empty image.
	 * @param path       The path this image is read from
	  * @param readClass Class through which the resource is read from.
	  *                  Leave to None when reading files outside program resources. (Default = None)
	 * @return Read image, which may be empty
	 */
	def readOrEmpty(path: Path, readClass: Option[Class[_]] = None) = readFrom(path, readClass) match
	{
		case Success(img) => img
		case Failure(_) => empty
	}
	
	/**
	  * Converts an awt image to Genesis image class
	  * @param awtImage An awt image (buffered images are prefferred because they can be simply wrapped)
	  * @return A genesis image
	  */
	def from(awtImage: java.awt.Image) =
	{
		awtImage match {
			case bufferedImage: BufferedImage => apply(bufferedImage) // Uses buffered image as is
			case otherType: java.awt.Image =>
				// Creates a new buffered image and draws original image on the new image
				val buffer = new BufferedImage(otherType.getWidth(null),
					otherType.getHeight(null), BufferedImage.TYPE_INT_ARGB)
				
				val g = buffer.createGraphics()
				g.drawImage(otherType, 0, 0, null)
				g.dispose()
				
				apply(buffer)
		}
	}
}

/**
  * This is a wrapper for the buffered image class
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1+
  */
case class Image private(private val source: Option[BufferedImage], scaling: Vector3D, alpha: Double,
						 private val _pixels: Lazy[PixelTable]) extends Scalable[Image]
{
	// ATTRIBUTES	----------------
	
	/**
	  * The size of the original image
	  */
	val sourceResolution = source.map { s => Size(s.getWidth, s.getHeight) }.getOrElse(Size.zero)
	
	/**
	  * @return The size of this image in pixels
	  */
	val size = sourceResolution * scaling
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return The pixels in this image
	  */
	def pixels = _pixels.get
	
	/**
	  * @return The width of this image in pixels
	  */
	def width = size.width
	
	/**
	  * @return The height of this image in pixels
	  */
	def height = size.height
	
	/**
	  * @return A copy of this image that isn't scaled above 100%
	  */
	def downscaled = if (scaling.dimensions2D.exists { _ > 1 }) withScaling(scaling.map { _ min 1 }) else this
	
	/**
	  * @return A copy of this image that isn't scaled below 100%
	  */
	def upscaled = if (scaling.dimensions2D.exists { _ < 1 }) withScaling(scaling.map { _ max 1 }) else this
	
	/**
	  * @return A copy of this image with original (100%) scaling
	  */
	def withOriginalSize = if (scaling == Vector3D.identity) this else withScaling(1)
	
	/**
	  * @return A copy of this image where x-axis is reversed
	  */
	def flippedHorizontally = mapPixelTable { _.flippedHorizontally }
	
	/**
	  * @return A copy of this image where y-axis is reversed
	  */
	def flippedVertically = mapPixelTable { _.flippedVertically }
	
	/**
	  * @return A copy of this image with increased contrast
	  */
	def withIncreasedContrast = IncreaseContrast(this)
	
	/**
	  * @return A copy of this image with inverted colors
	  */
	def inverted = Invert(this)
	
	/**
	  * If this image is downscaled, lowers the source image resolution to match the current size of this image. Will not
	  * affect non-scaled or upscaled images. Please note that <b>this operation cannot be reversed</b>
	  * @return A copy of this image with (possibly) lowered source resolution
	  */
	def withMinimumResolution = if (scaling.dimensions2D.forall { _ >= 1 }) this else
		withSourceResolution(size min sourceResolution, preserveUseSize = true)
	
	
	// IMPLEMENTED	----------------
	
	override def toString = s"Image ($size ${(alpha * 100).toInt}% Alpha)"
	
	
	// OPERATORS	----------------
	
	/**
	  * Scales this image
	  * @param scaling The scaling factor
	  * @return A scaled version of this image
	  */
	def *(scaling: VectorLike[_]): Image = withScaling(this.scaling * scaling)
	
	/**
	  * Scales this image
	  * @param scaling The scaling factor
	  * @return A scaled version of this image
	  */
	override def *(scaling: Double): Image = this * Vector3D(scaling, scaling)
	
	/**
	  * Downscales this image
	  * @param divider The dividing factor
	  * @return A downscaled version of this image
	  */
	def /(divider: VectorLike[_]): Image = withScaling(scaling / divider)
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a copy of this image with adjusted alpha value (transparency)
	  * @param newAlpha The new alpha value for this image [0, 1]
	  * @return A new image
	  */
	def withAlpha(newAlpha: Double) = copy(alpha = newAlpha max 0 min 1)
	
	/**
	  * Creates a copy of this image with mapped alpha value
	  * @param f A funtion for mapping image max alpha
	  * @return A copy of this image with mapped alpha
	  */
	def mapAlpha(f: Double => Double) = withAlpha(f(alpha))
	
	/**
	  * Creates a copy of this image with mapped alpha value
	  * @param alphaMod An alpha modifier
	  * @return A copy of this image with modified alpha
	  */
	def timesAlpha(alphaMod: Double) = withAlpha(alpha * alphaMod)
	
	/**
	  * Takes a sub-image from this image (meaning only a portion of this image)
	  * @param area The relative area that is cut from this image
	  * @return The portion of this image within the relative area
	  */
	def subImage(area: Bounds) =
	{
		source.map { s => area.within(Bounds(Point.origin, size)).map { _ / scaling }.map {
			a => Image(s.getSubimage(a.x.toInt, a.y.toInt, a.width.toInt, a.height.toInt), scaling) }.getOrElse(
			Image(new BufferedImage(0, 0, s.getType))) }.getOrElse(this)
	}
	
	/**
	  * Converts this one image into a strip containing multiple parts. The splitting is done horizontally.
	  * @param numberOfParts The number of separate parts within this image
	  * @param marginBetweenParts The horizontal margin between the parts within this image in pixels (default = 0)
	  * @return A stip containing numberOfParts images, which all are sub-images of this image
	  */
	def split(numberOfParts: Int, marginBetweenParts: Int = 0) =
	{
		val subImageWidth = (width - marginBetweenParts * (numberOfParts - 1)) / numberOfParts
		val subImageSize = Size(subImageWidth, height)
		Strip((0 until numberOfParts).map {
			index => subImage(Bounds(Point(index * (subImageWidth + marginBetweenParts), 0), subImageSize)) }.toVector)
	}
	
	/**
	  * Draws this image using a specific drawer
	  * @param drawer A drawer
	  * @param position The position where this image's origin is drawn (default = (0, 0))
	  * @param origin The relative origin of this image (default = (0, 0) = top left corner)
	  * @return Whether this image was fully drawn
	  */
	def drawWith(drawer: Drawer, position: Point = Point.origin, origin: Point = Point.origin) =
	{
		source.forall { s =>
			val transformed = drawer.transformed(Transformation.translation(position.toVector - origin).scaled(scaling))
			if (alpha == 1.0)
				transformed.drawImage(s)
			else
				transformed.withAlpha(alpha).drawImage(s)
		}
	}
	
	/**
	  * @param scaling A scaling modifier applied to the original image
	  * @return A scaled version of this image
	  */
	def withScaling(scaling: Vector3D) = copy(scaling = scaling)
	
	/**
	  * @param scaling A scaling modifier applied to the original image
	  * @return A scaled version of this image
	  */
	def withScaling(scaling: Double): Image = withScaling(Vector3D(scaling, scaling))
	
	/**
	  * @param newSize The target size for this image
	  * @param preserveShape Whether image shape should be preserved (default = true) (if dimensions would be shifted
	  *                      while this is true, uses the smaller available scaling)
	  * @return A copy of this image scaled to match the target size (dimensions might not be preserved)
	  */
	def withSize(newSize: Size, preserveShape: Boolean = true) =
	{
		if (preserveShape)
			this * ((newSize.width / width) min (newSize.height / height))
		else
			this * (newSize / size)
	}
	
	/**
	  * Scales this image, preserving shape.
	  * @param area An area
	  * @return A copy of this image that matches the specified area, but may be larger if shape preservation demands it.
	  */
	def filling(area: Size) = if (size.nonZero) this * (area / size).dimensions2D.max else this
	
	/**
	  * Scales this image, preserving shape.
	  * @param area An area
	  * @return A copy of this image that matches the specified area, but may be smaller if shape preservation demands it.
	  */
	def fitting(area: Size) = if (size.nonZero) this * (area / size).dimensions2D.min else this
	
	/**
	  * @param area Target area (maximum)
	  * @return A copy of this image that is smaller or equal to the target area. Shape is preserved.
	  */
	def smallerThan(area: Size) = if (size.fitsInto(area)) this else fitting(area)
	
	/**
	  * @param area Target area (minimum)
	  * @return A copy of this image that is larger or equal to the target area. Shape is preserved.
	  */
	def largerThan(area: Size) = if (area.fitsInto(size)) this else filling(area)
	
	/**
	  * @param f A mapping function for pixel tables
	  * @return A copy of this image with mapped pixels
	  */
	def mapPixelTable(f: PixelTable => PixelTable) =
	{
		if (source.isDefined)
		{
			val newPixels = f(pixels)
			Image(Some(newPixels.toBufferedImage), scaling, alpha, Lazy(newPixels))
		}
		else
			this
	}
	
	/**
	  * @param f A function that maps pixel colors
	  * @return A copy of this image with mapped pixels
	  */
	def mapPixels(f: Color => Color) = mapPixelTable { _.map(f) }
	
	/**
	  * @param f A function that maps pixel colors, also taking relative pixel coordinate
	  * @return A copy of this image with mapped pixels
	  */
	def mapPixelsWithIndex(f: (Color, Point) => Color) = mapPixelTable { _.mapWithIndex(f) }
	
	/**
	  * @param area The mapped relative area
	  * @param f A function that maps pixel colors
	  * @return A copy of this image with pixels mapped within the target area
	  */
	def mapArea(area: Area2D)(f: Color => Color) = mapPixelsWithIndex {
		(c, p) => if (area.contains(p * scaling)) f(c) else c }
	
	/**
	  * Creates a blurred copy of this image
	  * @param intensity The blurring intensity [0, 1], defaults to 1
	  * @return A blurred version of this image
	  */
	def blurred(intensity: Int = 1) = Blur(intensity)(this)
	
	/**
	  * Creates a sharpened copy of this image
	  * @param intensity The sharpening intensity (default = 5)
	  * @return A sharpened copy of this image
	  */
	def sharpened(intensity: Double = 5) = Sharpen(intensity)(this)
	
	/**
	  * Creates a version of this image where the hue (color) of the image is shifted
	  * @param hueAdjust The amount of shift applied to color hue
	  * @return A new image with adjusted hue
	  */
	def withAdjustedHue(hueAdjust: Rotation) = mapPixels { _ + hueAdjust }
	
	/**
	  * Creates a version of this image with a certain hue range adjusted
	  * @param sourceHue The hue that is targeted
	  * @param sourceRange The width of the targeted hue (larger angle means more hues are affected)
	  * @param targetHue The new hue the source hue will become after transformation
	  * @return A copy of this image with adjusted hue
	  */
	def withAdjustedHue(sourceHue: Angle, sourceRange: Angle, targetHue: Angle) =
		HueAdjust(sourceHue, sourceRange, targetHue)(this)
	
	/**
	  * Creates a copy of this image where each color channel is limited to a certain number of values
	  * @param colorAmount The number of possible values for each color channel
	  * @return A copy of this image with limited color options
	  */
	def withThreshold(colorAmount: Int) = Threshold(colorAmount)(this)
	
	/**
	  * Applies a bufferedImageOp to this image, producing a new image
	  * @param op The operation that is applied
	  * @return A new image with operation applied
	  */
	def filterWith(op: BufferedImageOp) =
	{
		source.map { s =>
			val destination = new BufferedImage(s.getWidth, s.getHeight, s.getType)
			op.filter(s, destination)
			Image(destination, scaling)
			
		}.getOrElse(this)
	}
	
	/**
	 * @param hue Hue for every pixel in this image
	 * @return A new image with all pixels set to provided color. Original alpha channel is preserved, however.
	 */
	def withColorOverlay(hue: Color) = mapPixels { c => hue.withAlpha(c.alpha) }
	
	/**
	  * Creates a new image with altered source resolution. This method can be used when you wish to lower the original
	  * image's resolution to speed up pixelwise operations. If you simply wish to change how this image looks in the
	  * program, please use withSize instead
	  * @param newSize The new source size
	  * @param preserveUseSize Whether the resulting image should be scaled to match this image (default = false)
	  * @return A new image with altered source resolution
	  */
	def withSourceResolution(newSize: Size, preserveUseSize: Boolean = false) =
	{
		source.map { s =>
			// Won't copy into 0 or negative size
			if (newSize.isNegative)
				Image.empty
			else if (s.getWidth == newSize.width.toInt && s.getHeight == newSize.height.toInt)
				this
			else
			{
				val scaledImage = s.getScaledInstance(newSize.width.toInt, newSize.height.toInt, java.awt.Image.SCALE_SMOOTH)
				val scaledAsBuffered = scaledImage match
				{
					case b: BufferedImage => b
					case i =>
						val buffered = new BufferedImage(i.getWidth(null), i.getHeight(null),
							BufferedImage.TYPE_INT_ARGB)
						val writeGraphics = buffered.createGraphics()
						writeGraphics.drawImage(i, 0, 0, null)
						writeGraphics.dispose()
						
						buffered
				}
				
				if (preserveUseSize)
					Image(scaledAsBuffered, size.toVector / newSize)
				else
					Image(scaledAsBuffered)
			}
		}.getOrElse(this)
	}
	
	/**
	  * Creates a copy of this image where the source data is limited to a certain resolution. The use size and the
	  * aspect ratio of this image is preserved, however.
	  * @param maxResolution The maximum resolution allowed for this image
	  * @return A copy of this image with equal or smaller resolution than that specified
	  */
	def withMaxSourceResolution(maxResolution: Size) =
	{
		if (source.isDefined)
		{
			// Preserves shape
			val scale = (maxResolution.width / width) min (maxResolution.height / height)
			// Won't ever upscale
			if (scale < 1)
				withSourceResolution(size * scale, preserveUseSize = true)
			else
				this
		}
		else
			this
	}
}

private class NoImageReaderAvailableException(message: String) extends RuntimeException(message)