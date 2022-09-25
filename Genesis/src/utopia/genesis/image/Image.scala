package utopia.genesis.image

import utopia.flow.parse.AutoClose._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.NullSafe._
import utopia.flow.operator.LinearScalable
import utopia.flow.view.immutable.caching.{Lazy, LazyWrapper}
import utopia.flow.view.template.LazyLike
import utopia.paradigm.color.Color
import utopia.genesis.graphics.Drawer3
import utopia.genesis.image.transform.{Blur, HueAdjust, IncreaseContrast, Invert, Sharpen, Threshold}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis2D, Direction2D}
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.shape.shape2d.{Area2D, Bounds, Insets, Matrix2D, Point, Size, SizedLike, Vector2D, Vector2DLike}
import utopia.paradigm.shape.template.Dimensional
import utopia.genesis.util.Drawer
import utopia.paradigm.shape.shape1d.Vector1D
import utopia.paradigm.shape.template.VectorLike.V

import scala.math.Ordering.Double.TotalOrdering
import scala.util.{Failure, Success, Try}
import java.awt.image.{BufferedImage, BufferedImageOp}
import java.io.FileNotFoundException
import java.nio.file.{Files, Path}
import javax.imageio.ImageIO

object Image
{
	/**
	 * A zero sized image with no pixel data
	 */
	val empty = new Image(None, Vector2D.identity, 1.0, None, LazyWrapper(PixelTable.empty))
	
	/**
	  * Creates a new image
	  * @param image The original buffered image source
	  * @param scaling The scaling applied to the image
	  * @param alpha The maximum alpha value used when drawing this image [0, 1] (default = 1 = fully visible)
	  * @param origin The relative coordinate inside this image which is considered the drawing origin
	  *               (the (0,0) coordinate of this image). None if the origin should be left unspecified (default).
	  *               When unspecified, the (0,0) coordinate is placed at the top left corner.
	  *               Please notice that this origin is applied before scaling is applied, meaning that the specified
	  *               origin should always be in relation to the source resolution and not necessarily image size.
	  * @return A new image
	  */
	def apply(image: BufferedImage, scaling: Vector2D = Vector2D.identity, alpha: Double = 1.0,
			  origin: Option[Point] = None): Image =
		new Image(Some(image), scaling, alpha, origin, Lazy { PixelTable.fromBufferedImage(image) })
	
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
			// ImageIO and class may return null. Image is read through class, if one is provided
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
	  * @param awtImage An awt image (buffered images are preferred because they can be simply wrapped)
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
	
	/**
	  * Creates a new image by drawing
	  * @param size Size of the image
	  * @param draw A function that will draw the image contents. The drawer is clipped to image bounds and
	  *             (0,0) is at the image top left corner.
	  * @tparam U Arbitrary result type
	  * @return Drawn image
	  */
	def paint[U](size: Size)(draw: Drawer => U) =
	{
		// If some of the dimensions were 0, simply creates an empty image
		if (size.isPositive)
		{
			// Creates the new buffer image
			val buffer = new BufferedImage(size.width.round.toInt, size.height.round.toInt, BufferedImage.TYPE_INT_ARGB)
			// Draws on the image
			Drawer.use(buffer.createGraphics()) { draw(_) }
			// Wraps the buffer image
			Image(buffer)
		}
		else
			empty
	}
	
	/**
	  * Creates a new image by drawing
	  * @param size Size of the image
	  * @param draw A function that will draw the image contents. The drawer is clipped to image bounds and
	  *             (0,0) is at the image top left corner.
	  * @tparam U Arbitrary result type
	  * @return Drawn image
	  */
	def paint2[U](size: Size)(draw: Drawer3 => U) =
	{
		// If some of the dimensions were 0, simply creates an empty image
		if (size.isPositive)
		{
			// Creates the new buffer image
			val buffer = new BufferedImage(size.width.round.toInt, size.height.round.toInt, BufferedImage.TYPE_INT_ARGB)
			// Draws on the image
			Drawer3(buffer.createGraphics()).consume(draw)
			// Wraps the buffer image
			Image(buffer)
		}
		else
			empty
	}
	
	
	// NESTED	------------------------
	
	private class NoImageReaderAvailableException(message: String) extends RuntimeException(message)
}

/**
  * This is a wrapper for the buffered image class
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1
  */
case class Image private(override protected val source: Option[BufferedImage], override val scaling: Vector2D,
						 override val alpha: Double, override val specifiedOrigin: Option[Point],
						 private val _pixels: LazyLike[PixelTable])
	extends ImageLike with LinearScalable[Image] with SizedLike[Image]
{
	// ATTRIBUTES	----------------
	
	/**
	  * The size of the original image
	  */
	override val sourceResolution = source.map { s => Size(s.getWidth, s.getHeight) }.getOrElse(Size.zero)
	/**
	  * @return The size of this image in pixels
	  */
	override val size = sourceResolution * scaling
	/**
	  * The bounds of this image when origin and size are both counted
	  */
	override lazy val bounds = Bounds(-origin, size)
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return A copy of this image without a specified origin location
	  */
	def withoutSpecifiedOrigin = if (specifiesOrigin) copy(specifiedOrigin = None) else this
	/**
	  * @return A copy of this image where the origin is placed at the center of the image
	  */
	def withCenterOrigin = withSourceResolutionOrigin((sourceResolution / 2).toPoint)
	
	/**
	  * @return A copy of this image that isn't scaled above 100%
	  */
	def downscaled = if (scaling.dimensions.exists { _ > 1 }) withScaling(scaling.map { _ min 1 }) else this
	/**
	  * @return A copy of this image that isn't scaled below 100%
	  */
	def upscaled = if (scaling.dimensions.exists { _ < 1 }) withScaling(scaling.map { _ max 1 }) else this
	/**
	  * @return A copy of this image with original (100%) scaling
	  */
	def withOriginalSize = if (scaling == Vector2D.identity) this else withScaling(1)
	
	/**
	  * @return A copy of this image where x-axis is reversed
	  */
	def flippedHorizontally =
	{
		val flipped = mapPixelTable { _.flippedHorizontally }
		specifiedOrigin match
		{
			// If an origin has been specified, flips it as well
			case Some(oldOrigin) =>
				val newOrigin = Point(sourceResolution.width - oldOrigin.x, oldOrigin.y)
				flipped.withSourceResolutionOrigin(newOrigin)
			case None => flipped
		}
	}
	/**
	  * @return A copy of this image where y-axis is reversed
	  */
	def flippedVertically =
	{
		val flipped = mapPixelTable { _.flippedVertically }
		specifiedOrigin match
		{
			// If an origin has been specified, flips it as well
			case Some(oldOrigin) =>
				val newOrigin = Point(oldOrigin.x, sourceResolution.height - oldOrigin.y)
				flipped.withSourceResolutionOrigin(newOrigin)
			case None => flipped
		}
	}
	
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
	
	/**
	  * @return A buffered image copied from the source data of this image. None if this image is empty.
	  */
	def toAwt =
	{
		withMinimumResolution.source.map { img =>
			val colorModel = img.getColorModel
			val isAlphaPremultiplied = colorModel.isAlphaPremultiplied
			val raster = img.copyData(null)
			new BufferedImage(colorModel, raster, isAlphaPremultiplied, null)
				.getSubimage(0, 0, img.getWidth, img.getHeight)
		}
	}
	
	
	// IMPLEMENTED	----------------
	
	override def repr = this
	
	/**
	  * @return Whether this image is actually completely empty
	  */
	override def isEmpty = source.isEmpty
	
	override def preCalculatedPixels = _pixels.current
	/**
	  * @return The pixels in this image
	  */
	override def pixels = _pixels.value
	
	override def *(scaling: Double): Image = withScaling(this.scaling * scaling)
	
	override def withSize(size: Size) = this * (size / this.size)
	
	override def croppedToFitWithin(maxArea: Vector2DLike[_ <: V]) = {
		if (fitsWithin(maxArea))
			this
		else {
			val requiredCropping = size - maxArea
			crop(Insets.symmetric(requiredCropping))
		}
	}
	override def croppedToFitWithin(maxLength: Vector1D) = {
		if (fitsWithin(maxLength))
			this
		else {
			val requiredCropping = lengthAlong(maxLength.axis) - maxLength.length
			crop(Insets.symmetric(Vector1D(requiredCropping, maxLength.axis)))
		}
	}
	
	
	// OPERATORS	----------------
	
	/**
	  * Scales this image
	  * @param scaling The scaling factor
	  * @return A scaled version of this image
	  */
	def *(scaling: Dimensional[Double]): Image = withScaling(this.scaling * scaling)
	/**
	  * Downscales this image
	  * @param divider The dividing factor
	  * @return A downscaled version of this image
	  */
	def /(divider: Dimensional[Double]): Image = withScaling(scaling / divider)
	
	/**
	 * @param other Another image
	 * @return A copy of this image with the other image drawn on top of this one with its origin at the point
	  *         of this image's origin (if specified)
	 */
	def +(other: Image) = withOverlay(other)
	
	
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
	  * @param newOrigin A new image origin relative to the top left coordinate in the source resolution image
	  * @return A copy of this image with the specified origin
	  */
	def withSourceResolutionOrigin(newOrigin: Point) = copy(specifiedOrigin = Some(newOrigin))
	/**
	  * @param f A mapping function for source resolution origin
	  * @return A copy of this image with mapped origin
	  */
	def mapSourceResolutionOrigin(f: Point => Point) = withSourceResolutionOrigin(f(sourceResolutionOrigin))
	/**
	  * @param translation A translation applied to source resolution image origin
	  * @return A copy of this image with translated origin
	  */
	def withTranslatedSourceResolutionOrigin(translation: Dimensional[Double]) =
		mapSourceResolutionOrigin { _+ translation }
	/**
	  * @param newOrigin A new image origin <b>relative to the current image size</b>, which is scaling-dependent
	  * @return A copy of this image with the specified origin
	  */
	def withOrigin(newOrigin: Point) = withSourceResolutionOrigin(newOrigin / scaling)
	/**
	  * @param f A mapping function for applied image origin
	  * @return A copy of this image with mapped origin
	  */
	def mapOrigin(f: Point => Point) = withOrigin(f(origin))
	/**
	  * @param translation Translation applied to current (scaled) image origin
	  * @return A copy of this image with translated origin
	  */
	def withTranslatedOrigin(translation: Dimensional[Double]) = mapOrigin { _ + translation }
	
	/**
	  * Takes a sub-image from this image (meaning only a portion of this image)
	  * @param area The relative area that is cut from this image. The (0,0) is considered to be at the top left
	  *             corner of this image.
	  * @return The portion of this image within the relative area
	  */
	def subImage(area: Bounds) = source match {
		case Some(source) =>
			area.intersectionWith(Bounds(Point.origin, size)) match {
				case Some(overlap) => _subImage(source, overlap / scaling)
				case None => Image(new BufferedImage(0, 0, source.getType), scaling, alpha,
					specifiedOrigin.map { _ - area.position / scaling })
			}
		case None => this
	}
	// Only works when specified area is inside the original image's bounds
	private def _subImage(img: BufferedImage, relativeArea: Bounds) =
	{
		val newSource = img.getSubimage(relativeArea.x.toInt, relativeArea.y.toInt, relativeArea.width.toInt,
			relativeArea.height.toInt)
		Image(newSource, scaling, alpha, specifiedOrigin.map { _ - relativeArea.position })
	}
	
	/**
	  * Crops this image from the sides
	  * @param insets Insets to crop out of this image
	  * @return A cropped copy of this image
	  */
	def crop(insets: Insets) = {
		source match {
			case Some(img) =>
				val totalInsets = insets.total
				if (totalInsets.width > width || totalInsets.height > height)
					Image.empty
				else
					_subImage(img, (Bounds(Point.origin, size) - insets) / scaling)
			case None => this
		}
	}
	/**
	  * @param side Side from which to crop from this image
	  * @param amount Amount of pixels to crop from this image
	  * @return A cropped copy of this image
	  */
	def cropFromSide(side: Direction2D, amount: Double) = crop(Insets.towards(side, amount))
	
	/**
	  * Converts this one image into a strip containing multiple parts. The splitting is done horizontally.
	  * @param numberOfParts The number of separate parts within this image
	  * @param marginBetweenParts The horizontal margin between the parts within this image in pixels (default = 0)
	  * @return A strip containing numberOfParts images, which all are sub-images of this image
	  */
	def split(numberOfParts: Int, marginBetweenParts: Int = 0) = {
		val subImageWidth = (width - marginBetweenParts * (numberOfParts - 1)) / numberOfParts
		val subImageSize = Size(subImageWidth, height)
		Strip((0 until numberOfParts).map {
			index => subImage(Bounds(Point(index * (subImageWidth + marginBetweenParts)), subImageSize)) }.toVector)
	}
	
	/**
	  * @param scaling A scaling modifier applied to the original image
	  * @return A scaled version of this image
	  */
	def withScaling(scaling: Vector2D) = copy(scaling = scaling)
	/**
	  * @param scaling A scaling modifier applied to the original image
	  * @return A scaled version of this image
	  */
	def withScaling(scaling: Dimensional[Double]): Image = withScaling(Vector2D.withDimensions(scaling.dimensions))
	/**
	  * @param scaling A scaling modifier applied to the original image
	  * @return A scaled version of this image
	  */
	def withScaling(scaling: Double): Image = withScaling(Vector2D(scaling, scaling))
	
	/**
	  * @param newSize The target size for this image
	  * @param preserveShape Whether image shape should be preserved (default = true) (if dimensions would be shifted
	  *                      while this is true, uses the smaller available scaling)
	  * @return A copy of this image scaled to match the target size (dimensions might not be preserved)
	  */
	def withSize(newSize: Size, preserveShape: Boolean = true) = {
		if (preserveShape)
			this * ((newSize.width / width) min (newSize.height / height))
		else
			this * (newSize / size)
	}
	
	/**
	  * Scales this image, preserving shape.
	  * @param area An area
	  * @return A copy of this image that matches the specified area, but may be smaller if shape preservation demands it.
	  */
	@deprecated("Please use fittingWithin(Vector2DLike, Boolean) instead", "v3.1")
	def fitting(area: Size) = if (size.nonZero) this * (area / size).dimensions2D.min else this
	
	/**
	  * @param area Target area (maximum)
	  * @return A copy of this image that is smaller or equal to the target area. Shape is preserved.
	  */
	@deprecated("Please use fittingWithin(Vector2DLike) instead", "v3.1")
	def smallerThan(area: Size) = if (size.fitsWithin(area)) this else fitting(area)
	/**
	  * @param area Target area (minimum)
	  * @return A copy of this image that is larger or equal to the target area. Shape is preserved.
	  */
	@deprecated("Please use filling(Dimensional) instead", "v3.1")
	def largerThan(area: Size) = filling(area)
	
	/**
	  * Limits the height or width of this image
	  * @param side Targeted side / axis
	  * @param maxLength Maximum length for this image on that axis
	  * @return A copy of this image that has equal or lower than maximum length on the specified axis
	  */
	@deprecated("Please use fittingWithin(Vector1D) instead", "v3.1")
	def limitedAlong(side: Axis2D, maxLength: Double) =
		if (size.along(side) <= maxLength) this else smallerThan(size.withDimension(side(maxLength)))
	/**
	  * @param maxWidth Maximum allowed width
	  * @return A copy of this image with equal or lower width than the specified maximum
	  */
	@deprecated("Please use fittingWithinWidth(Double) instead", "v3.1")
	def withLimitedWidth(maxWidth: Double) = limitedAlong(X, maxWidth)
	/**
	  * @param maxHeight Maximum allowed height
	  * @return A copy of this image with equal or lower height than the specified maximum
	  */
	@deprecated("Please use fittingWithinHeight(Double) instead", "v3.1")
	def withLimitedHeight(maxHeight: Double) = limitedAlong(Y, maxHeight)
	
	/**
	  * @param f A mapping function for pixel tables
	  * @return A copy of this image with mapped pixels
	  */
	def mapPixelTable(f: PixelTable => PixelTable) = {
		if (source.isDefined) {
			val newPixels = f(pixels)
			Image(Some(newPixels.toBufferedImage), scaling, alpha, specifiedOrigin, LazyWrapper(newPixels))
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
	def blurred(intensity: Double = 1) = Blur(intensity)(this)
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
	def filterWith(op: BufferedImageOp) = source match {
		case Some(source) =>
			val destination = new BufferedImage(source.getWidth, source.getHeight, source.getType)
			op.filter(source, destination)
			Image(destination, scaling, alpha, specifiedOrigin)
		case None => this
	}
	
	/**
	 * @param hue Hue for every pixel in this image
	 * @return A new image with all pixels set to provided color. Original alpha channel is preserved, however.
	 */
	def withColorOverlay(hue: Color) = mapPixels { c => hue.withAlpha(c.alpha) }
	
	/**
	  * Creates a new image with altered source resolution. This method can be used when you wish to lower the original
	  * image's resolution to speed up pixel-wise operations. If you simply wish to change how this image looks in the
	  * program, please use withSize instead
	  * @param newSize The new source size
	  * @param preserveUseSize Whether the resulting image should be scaled to match this image (default = false)
	  * @return A new image with altered source resolution
	  */
	def withSourceResolution(newSize: Size, preserveUseSize: Boolean = false) = {
		source match {
			case Some(source) =>
				// Won't copy into 0 or negative size
				if (newSize.isNegative)
					Image.empty
				else if (source.getWidth == newSize.width.toInt && source.getHeight == newSize.height.toInt)
					this
				else {
					val scaledImage = source.getScaledInstance(newSize.width.toInt, newSize.height.toInt,
						java.awt.Image.SCALE_SMOOTH)
					val scaledAsBuffered = scaledImage match {
						case b: BufferedImage => b
						case i =>
							val buffered = new BufferedImage(i.getWidth(null), i.getHeight(null),
								BufferedImage.TYPE_INT_ARGB)
							val writeGraphics = buffered.createGraphics()
							writeGraphics.drawImage(i, 0, 0, null)
							writeGraphics.dispose()
							
							buffered
					}
					val newOrigin = specifiedOrigin.map { _ * (newSize / sourceResolution) }
					
					if (preserveUseSize)
						Image(scaledAsBuffered, size.toVector / newSize, alpha, newOrigin)
					else
						Image(scaledAsBuffered, Vector2D.identity, alpha, newOrigin)
				}
			case None => this
		}
	}
	
	/**
	  * Creates a copy of this image where the source data is limited to a certain resolution. The use size and the
	  * aspect ratio of this image are preserved, however.
	  * @param maxResolution The maximum resolution allowed for this image
	  * @return A copy of this image with equal or smaller resolution than that specified
	  */
	def withMaxSourceResolution(maxResolution: Size) = {
		if (source.isDefined) {
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
	
	/**
	 * @param overlayImage An image that will be drawn on top of this image
	  * @param overlayPosition A position <b>relative to this image's origin<b> that determines where the origin of
	  *                        the other image will be placed
	 * @return A new image where the specified image is drawn on top of this one
	 */
	def withOverlay(overlayImage: Image, overlayPosition: Point = Point.origin) = {
		// If one of the images is empty, uses the other image
		if (overlayImage.isEmpty)
			this
		else
			toAwt match
			{
				case Some(buffer) =>
					// Draws the other image on the buffer
					// Determines the scaling to apply, if necessary
					Drawer.use(buffer.createGraphics()) { d =>
						(overlayImage / scaling).drawWith(d.clippedTo(Bounds(Point.origin, sourceResolution)),
							sourceResolutionOrigin + overlayPosition)
					}
					// Wraps the result
					Image(buffer, scaling, alpha, specifiedOrigin)
					
				case None => overlayImage
			}
	}
	
	/**
	  * @param paint A function for painting over this image. Accepts a drawer that is clipped to this image's area
	  *              ((0,0) is at the top left corner if this image).
	  * @return A copy of this image with the paint operation applied
	  */
	def paintedOver(paint: Drawer => Unit) = {
		toAwt match {
			case Some(buffer) =>
				// Paints into created buffer
				Drawer.use(buffer.createGraphics()) { d => paint(d.clippedTo(Bounds(Point.origin, sourceResolution))) }
				Image(buffer, scaling, alpha, specifiedOrigin)
			case None => this
		}
	}
	
	/**
	  * @param paint A function for painting over this image. Accepts a drawer that is clipped to this image's area
	  *              ((0,0) is at the top left corner if this image).
	  * @return A copy of this image with the paint operation applied
	  */
	def paintedOver2[U](paint: Drawer3 => U) = {
		toAwt match {
			case Some(buffer) =>
				// Paints into created buffer
				Drawer3(buffer.createGraphics())
					.consume { d => paint(d.withClip(Bounds(Point.origin, sourceResolution))) }
				Image(buffer, scaling, alpha, specifiedOrigin)
			case None => this
		}
	}
	
	/**
	  * Draws this image on an empty image with predefined size. Places this image at the center of the other image.
	  * If the new image size is smaller than the (scaled) size of this image, crops some parts of this image out.
	  * Also, if this image contains an alpha modifier, that modifier is applied directly into the specified image's
	  * source pixels.
	  * @param targetSize The source resolution size of the resulting image
	  * @return A new image with this image drawn at the center. The image origin is preserved, if it was defined
	  *         in this image.
	  */
	def paintedToCanvas(targetSize: Size) = {
		if (size == targetSize)
			this
		else
		{
			// Creates the new buffer image
			val buffer = new BufferedImage(targetSize.width.round.toInt, targetSize.height.round.toInt,
				BufferedImage.TYPE_INT_ARGB)
			// Draws this image to the center of the image
			val topLeftPosition = (targetSize - size).toPoint / 2
			val originDrawPosition = topLeftPosition + origin
			Drawer.use(buffer.createGraphics()) { drawer => drawWith(drawer, originDrawPosition) }
			// Wraps the image
			Image(buffer, origin = if (specifiesOrigin) Some(originDrawPosition) else None)
		}
	}
	
	/**
	  * Transforms this image using specified transformation. The resulting image is based on a drawn copy of this image.
	  * @param transformation Transformation to apply to this image
	  * @return Transformed copy of this image
	  */
	def transformedWith(transformation: Matrix2D) = {
		if (isEmpty)
			this
		else {
			// Calculates new bounds
			val transformedBounds = (bounds * transformation).bounds
			val transformedOrigin = Point.origin * transformation
			
			// Creates the buffer image
			val buffer = new BufferedImage(transformedBounds.width.round.toInt, transformedBounds.height.round.toInt,
				BufferedImage.TYPE_INT_ARGB)
			
			// Draws on the buffer
			Drawer.use(buffer.createGraphics()) { d =>
				drawWith(d, transformedOrigin - transformedBounds.topLeft, Some(transformation))
			}
			// Wraps the image
			Image(buffer, origin = if (specifiesOrigin) Some(transformedOrigin - transformedBounds.topLeft) else None)
		}
	}
	
	/**
	 * Writes this image to a file
	 * @param filePath Path where the file is written
	 * @return Success or failure
	 */
	def writeToFile(filePath: Path) = filePath.createParentDirectories().flatMap { _ =>
		Try[Unit] {
			source match
			{
				case Some(source) => ImageIO.write(source, filePath.fileType, filePath.toFile)
				case None => if (!filePath.exists) Files.createFile(filePath)
			}
		}
	}
}