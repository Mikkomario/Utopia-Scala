package utopia.genesis.image

import utopia.flow.collection.immutable.{Matrix, Pair}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.Use
import utopia.flow.view.immutable.caching.{Lazy, PreInitializedLazy}
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.image.transform.{Blur, HueAdjust, IncreaseContrast, Invert, Sharpen, Threshold}
import utopia.paradigm.angular.{Angle, DirectionalRotation}
import utopia.paradigm.color.ColorShade.Dark
import utopia.paradigm.color.{Color, ColorShade}
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.enumeration.{Alignment, Direction2D}
import utopia.paradigm.shape.shape1d.Dimension
import utopia.paradigm.shape.shape1d.vector.Vector1D
import utopia.paradigm.shape.shape2d._
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.{Size, Sized}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.transform.{Adjustment, LinearSizeAdjustable}

import java.awt.image.{BufferedImage, BufferedImageOp}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, FileNotFoundException, IOException, InputStream, OutputStream}
import java.nio.file.{Files, Path}
import java.util.Base64
import javax.imageio.ImageIO
import scala.util.{Failure, Success, Try}

object Image
{
	/**
	 * A zero sized image with no pixel data
	 */
	val empty = new Image(None, Vector2D.identity, 1.0, None, PreInitializedLazy(Pixels.empty),
		PreInitializedLazy(Dark))
	
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
	{
		val lazyPixels = Lazy { Pixels.fromBufferedImage(image, lazily = true) }
		new Image(Some(image), scaling, alpha, origin, lazyPixels, lazyPixels.map { _.averageShade })
	}
	
	/**
	  * @param pixels A set of pixels
	  * @return An image based on those pixels
	  */
	def fromPixels(pixels: Pixels) = pixels.notEmpty match {
		case Some(pixels) =>
			apply(Some(pixels.toBufferedImage), Vector2D.identity, 1.0, None, PreInitializedLazy(pixels),
				Lazy { pixels.averageShade })
		case None => empty
	}
	
	/**
	  * Reads an image from a file
	  * @param path The path the image is read from
	  * @param readClass Class through which the resource is read from.
	  *                  Leave to None when reading files outside program resources. (Default = None)
	  * @return The read image wrapped in Try
	  */
	def readFrom(path: Path, readClass: Option[Class[_]] = None) = {
		// Checks that file exists (not available with class read method)
		if (readClass.isDefined || Files.exists(path)) {
			// ImageIO and class may return null. Image is read through class, if one is provided
			val readResult = Try { readClass.map { c => Option(c.getResourceAsStream(s"/${ path.toString }"))
				.flatMap { _.consume { stream => Option(ImageIO.read(stream)) } } }
				.getOrElse { Option(ImageIO.read(path.toFile)) } }
			
			readResult.flatMap {
				case Some(result) => Success(apply(result))
				case None => Failure(new NoImageReaderAvailableException(s"Cannot read image from file: ${ path.toString }"))
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
	def readOrEmpty(path: Path, readClass: Option[Class[_]] = None) = readFrom(path, readClass) match {
		case Success(img) => img
		case Failure(_) => empty
	}
	
	/**
	  * Converts an awt image to Genesis image class
	  * @param awtImage An awt image (buffered images are preferred because they can be simply wrapped)
	  * @return A genesis image
	  */
	def from(awtImage: java.awt.Image) = {
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
	  * Parses an image from its Base 64 encoded format.
	  * @param imageString A Base 64 encoded string which represents an image
	  * @return Parsed image. Failure if parsing failed.
	  */
	def fromBase64EncodedString(imageString: String) = Try {
		val imageBytes = Base64.getDecoder.decode(imageString)
		new ByteArrayInputStream(imageBytes).consume { stream => apply(ImageIO.read(stream)) }
	}
	
	/**
	  * Reads an image from an input stream.
	  * NB: Doesn't close the specified stream.
	  * @param stream A stream to read into an image
	  * @return An image read from the specified stream. Failure if image-reading failed.
	  */
	def fromStream(stream: InputStream) = Try { apply(ImageIO.read(stream)) }
	
	/**
	  * Creates a new image by drawing
	  * @param size Size of the image
	  * @param draw A function that will draw the image contents. The drawer is clipped to image bounds and
	  *             (0,0) is at the image top left corner.
	  * @tparam U Arbitrary result type
	  * @return Drawn image
	  */
	def paint[U](size: Size)(draw: Drawer => U) = {
		// If some of the dimensions were 0, simply creates an empty image
		if (size.sign.isPositive) {
			// Creates the new buffer image
			val buffer = new BufferedImage(size.width.round.toInt, size.height.round.toInt, BufferedImage.TYPE_INT_ARGB)
			// Draws on the image
			Drawer(buffer.createGraphics()).consume(draw)
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
						 private val _pixels: Lazy[Pixels], private val _shade: Lazy[ColorShade])
	extends ImageLike with LinearSizeAdjustable[Image] with Sized[Image] with MaybeEmpty[Image]
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
	 * @return A copy of this image that contains minimum amount of pixels.
	 *         I.e. empty (invisible) rows and columns are removed from the edges.
	 *         The origin of this image is preserved, if defined.
	 */
	def cropped = {
		val px = pixels
		// Finds the first row that contains visible pixels
		px.rowIndices.find { y => px.columnIndices.exists { x => px(x, y).visible } } match {
			case Some(minY) =>
				// Finds the last row that contains visible pixels
				val maxY = px.rowIndices.findLast { y => px.columnIndices.exists { x => px(x, y).visible } }.get
				val colRange = minY to maxY
				// Finds the first and last column that contain visible pixels
				val minX = px.columnIndices.find { x => colRange.exists { y => px(x, y).visible } }.get
				val maxX = px.columnIndices.findLast { x => colRange.exists { y => px(x, y).visible } }.get
				// Returns the cropped image, preserves the origin
				crop(Insets(minX - 1, px.width - maxX - 2, minY - 1, px.height - maxY - 2) * scaling)
			// Case: No visible pixels found => returns an empty image
			case None => Image.empty
		}
	}
	
	/**
	  * @return A copy of this image where x-axis is reversed
	  */
	def flippedHorizontally = {
		val flipped = _mapPixels(preserveShade = true) { _.flippedHorizontally }
		specifiedOrigin match {
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
	def flippedVertically = {
		val flipped = _mapPixels(preserveShade = true) { _.flippedVertically }
		specifiedOrigin match {
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
	def withMinimumResolution = if (scaling.dimensions.forall { _ >= 1 }) this else
		withSourceResolution(size topLeft sourceResolution, preserveUseSize = true)
	
	/**
	  * @return A buffered image copied from the source data of this image. None if this image is empty.
	  */
	def toAwt = {
		withMinimumResolution.source.map { img =>
			val colorModel = img.getColorModel
			val isAlphaPremultiplied = colorModel.isAlphaPremultiplied
			val raster = img.copyData(null)
			new BufferedImage(colorModel, raster, isAlphaPremultiplied, null)
				.getSubimage(0, 0, img.getWidth, img.getHeight)
		}
	}
	
	/**
	  * @return A Base 64 encoded string based on this image's contents.
	  *         A failure if image-writing failed.
	  *         Contains an empty string if this image was empty.
	  */
	def toBase64String = encodeToBase64()
	
	
	// IMPLEMENTED	----------------
	
	override def self = this
	
	/**
	  * @return Whether this image is actually completely empty
	  */
	override def isEmpty = source.isEmpty || size.dimensions.exists { _ < 1.0 }
	override def nonEmpty = !isEmpty
	
	override def pixels = _pixels.value
	override def shade: ColorShade = _shade.value
	
	override def *(scaling: Double): Image = withScaling(this.scaling * scaling)
	
	override def withSize(size: Size) = this * (size / this.size)
	
	override def croppedToFitWithin(maxArea: HasDoubleDimensions) = {
		if (fitsWithin(maxArea))
			this
		else {
			val requiredCropping = size - maxArea
			crop(Insets.symmetric(requiredCropping))
		}
	}
	override def croppedToFitWithin(maxLength: Dimension[Double]) = {
		if (fitsWithin(maxLength))
			this
		else {
			val requiredCropping = lengthAlong(maxLength.axis) - maxLength.value
			crop(Insets.symmetric(Vector1D(requiredCropping, maxLength.axis)))
		}
	}
	
	
	// OTHER	----------------
	
	/**
	  * Scales this image
	  * @param scaling The scaling factor
	  * @return A scaled version of this image
	  */
	def *(scaling: HasDoubleDimensions): Image = withScaling(this.scaling * scaling)
	/**
	  * Downscales this image
	  * @param divider The dividing factor
	  * @return A downscaled version of this image
	  */
	def /(divider: HasDoubleDimensions): Image = withScaling(scaling / divider)
	
	/**
	 * @param other Another image
	 * @return A copy of this image with the other image drawn on top of this one with its origin at the point
	  *         of this image's origin (if specified)
	 */
	def +(other: Image) = withOverlay(other)
	
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
	def withTranslatedSourceResolutionOrigin(translation: HasDoubleDimensions) =
		mapSourceResolutionOrigin { _ + translation }
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
	def withTranslatedOrigin(translation: HasDoubleDimensions) = mapOrigin { _ + translation }
	
	/**
	  * Takes a sub-image from this image (meaning only a portion of this image)
	  * @param area The relative area that is cut from this image. The (0,0) is considered to be at the top left
	  *             corner of this image.
	  * @return The portion of this image within the relative area
	  */
	def subImage(area: Bounds) = source match {
		case Some(source) =>
			area.overlapWith(Bounds(Point.origin, size)) match {
				case Some(overlap) => _subImage(source, overlap / scaling)
				case None => Image(new BufferedImage(0, 0, source.getType), scaling, alpha,
					specifiedOrigin.map { _ - area.position / scaling })
			}
		case None => this
	}
	// Only works when specified area is inside the original image's bounds and scaled according to source resolution
	private def _subImage(img: BufferedImage, relativeArea: Bounds) = {
		val area = relativeArea.round
		val newSource = img.getSubimage(area.leftX.toInt, area.topY.toInt, area.width.toInt, area.height.toInt)
		val newLazyPixels = Lazy { _pixels.value.view(area) }
		new Image(Some(newSource), scaling, alpha, specifiedOrigin.map { _ - area.position },
			newLazyPixels, newLazyPixels.map { _.averageShade })
	}
	
	/**
	  * Crops this image from the sides
	  * @param insets Insets to crop out of this image
	  * @return A cropped copy of this image
	  */
	def crop(insets: Insets) = source match {
		case Some(img) =>
			val positiveInsets = insets.positive
			val totalInsets = positiveInsets.total
			if (totalInsets.width >= width || totalInsets.height >= height)
				Image.empty
			else
				_subImage(img, (Bounds(Point.origin, size) - positiveInsets) / scaling)
		case None => this
	}
	/**
	  * @param side Side from which to crop from this image
	  * @param amount Amount of pixels to crop from this image
	  * @return A cropped copy of this image
	  */
	def cropFromSide(side: Direction2D, amount: Double) = crop(Insets.towards(side, amount))
	
	/**
	  * Increases or decreases the size of the canvas around this image.
	  * In other words, modifies this image's size without scaling it.
	  * If the size gets smaller, crops this image. If it gets larger, adds padding.
	  * @param canvasSize New canvas size
	  * @param alignment Alignment that determines how the padding or cropping is applied.
	  *                  E.g. If set to Center (default), applies the padding or cropping symmetrically.
	  *                  If set to BottomLeft, on the other hand, the padding or cropping would be applied on the
	  *                  right and top edges only.
	  * @return Copy of this image with the new size
	  */
	def withCanvasSize(canvasSize: Size, alignment: Alignment = Center) = {
		// Case: Preserving size => No change is necessary
		if (canvasSize == size)
			this
		// Case: Size gets smaller => Crops
		else if (canvasSize.forAllDimensionsWith(size) { _ <= _ })
			crop(alignment.surroundWith(size - canvasSize))
		// Case: Size gets larger on at least one side => Repaints with padding
		else
			source match {
				case Some(raw) =>
					// Performs the changes in the original scaling
					val newSourceSize = (canvasSize / scaling).round
					// Paints the new image
					val modified = Image
						.paint(newSourceSize) { drawer =>
							drawer.drawAwtImage(raw, alignment.position(sourceResolution, newSourceSize).toPoint)
						}
						.copy(scaling = (canvasSize / newSourceSize).toVector2D)
					// Case: Size increased on all sides => We know that the image shade is fully preserved, also
					if (canvasSize.forAllDimensionsWith(size) { _ >= _ })
						modified.copy(_shade = _shade)
					// Case: Cropping plus padding at the same time
					// => We don't know the resulting shade without calculating it
					else
						modified
				// Case: Empty image (no awt source) => No need to paint anything
				case None => withSize(canvasSize)
			}
	}
	
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
	def withScaling(scaling: HasDoubleDimensions): Image = copy(scaling = Vector2D.from(scaling))
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
	  * @param area Target area (minimum)
	  * @return A copy of this image that is larger or equal to the target area. Shape is preserved.
	  */
	@deprecated("Please use filling(Dimensional) instead", "v3.1")
	def largerThan(area: Size) = filling(area)
	
	/**
	  * @param f A mapping function for pixel tables
	  * @return A copy of this image with mapped pixels
	  */
	def mapPixels(f: Pixels => Pixels) = _mapPixels(preserveShade = false)(f)
	private def _mapPixels(preserveShade: Boolean)(f: Pixels => Pixels) = {
		if (source.isDefined) {
			val newPixels = f(pixels)
			Image(Some(newPixels.toBufferedImage), scaling, alpha, specifiedOrigin, PreInitializedLazy(newPixels),
				if (preserveShade && _shade.isInitialized) _shade else Lazy { newPixels.averageShade })
		}
		else
			this
	}
	/**
	  * @param f A mapping function for pixel tables
	  * @return A copy of this image with mapped pixels
	  */
	@deprecated("Please use .mapPixels instead", "v3.2")
	def mapPixelTable(f: Pixels => Pixels) = mapPixels(f)
	/**
	  * @param f A function that maps pixel colors
	  * @return A copy of this image with mapped pixels
	  */
	def mapEachPixel(f: Color => Color) = mapPixels { _.map(f) }
	/**
	  * @param f A function that maps pixel colors, also taking relative pixel coordinate
	  * @return A copy of this image with mapped pixels
	  */
	def mapPixelsWithIndex(f: (Color, Pair[Int]) => Color) = mapPixels { _.mapWithIndex(f) }
	/**
	  * @param f A function that maps pixel colors, also taking relative pixel coordinate
	  * @return A copy of this image with mapped pixels
	  */
	def mapPixelPoints(f: (Color, Point) => Color) = mapPixels { _.mapPoints(f) }
	/**
	  * @param area The mapped relative area
	  * @param f A function that maps pixel colors
	  * @return A copy of this image with pixels mapped within the target area
	  */
	def mapArea(area: Area2D)(f: Color => Color) =
		mapPixelPoints { (c, p) => if (area.contains(p * scaling)) f(c) else c }
	
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
	def withAdjustedHue(hueAdjust: DirectionalRotation) = mapEachPixel { _ + hueAdjust }
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
	  * Extracts the parts of this image that are placed at the edges between transparent and non-transparent areas.
	  * @param strokeWidth Width of the edge that is formed.
	  *                    Default = 1, meaning that only pixels adjacent to more transparent pixels will be preserved.
	  * @param alphaThreshold The modifier that is applied to the compared alpha values when considering whether there
	  *                       is and edge [0,1[.
	  *                       0 Means that there must be a fully transparent surrounding pixel,
	  *                       whereas 0.4 means that the surrounding pixel must be 60+% more transparent.
	  *                       Default = 0.7 = the surrounding pixel must be 30+% more transparent.
	  * @return Copy of this image where only edge pixels have been preserved.
	  *         The other pixels are replaced with fully transparent pixels.
	  */
	def extractEdges(strokeWidth: Int = 1, alphaThreshold: Double = 0.7) =
		highlightEdgesWith(strokeWidth = strokeWidth,
			onlyEdges = true) { _.alpha < _.alpha * alphaThreshold } { (_, c) => c }
	/**
	  * Draws the edges of this image using the specified color / draw-settings
	  * @param alphaThreshold Minimum difference in alpha values that qualifies as an edge.
	  *                       Default = 30% decrease in alpha values.
	  * @param placeInside Whether the edge should be placed inside the image area (true),
	  *                    or to surround the image area (false, default).
	  * @param onlyEdges Whether non-edge pixels should be replaced with fully transparent pixels.
	  *                  Default = false = non-edge pixels will be kept as is.
	  * @param stroke Stroke settings that determine the width of the drawn edge, as well as its color.
	  * @return Copy of this image with edges drawn around or inside it, depending on the 'placeInside' parameter.
	  *         If 'onlyEdges' parameter is set to true, only the drawn edges will be returned.
	  */
	def paintEdges(alphaThreshold: Adjustment = Adjustment(0.3), placeInside: Boolean = false,
	               onlyEdges: Boolean = false)
	              (implicit stroke: StrokeSettings) =
	{
		val findEdge: (Color, Color) => Boolean = {
			if (placeInside)
				(outer, inner) => { outer.alpha < inner.alpha * alphaThreshold(-1) }
			else
				(outer, inner) => { outer.alpha > inner.alpha * alphaThreshold(1) }
		}
		highlightEdgesWith(strokeWidth = stroke.strokeWidth.toInt,
			onlyEdges = onlyEdges)(findEdge){ (_, _) => stroke.color }
	}
	/**
	  * A generic function for detecting and modifying the edges in this image
	  * @param reduceOrdering Ordering to use when selecting the edge comparison pixel.
	  *                       None if any edge pixel will do (default).
	  *                       The selected pixel will be passed to the 'createPixel' function.
	  * @param strokeWidth Width of the "stroke" when detecting edges.
	  *                    This value determines how far a pixel can be from a comparative pixel and still count
	  *                    as an edge.
	  *                    The default value is 1, meaning that the pixels must be adjacent in order to form an edge.
	  * @param onlyEdges Whether non-edge areas of this image should be removed
	  *                  (i.e. replaced with fully transparent pixels)
	  * @param isEdge A function that compares two pixel color values and determines whether there exists an edge
	  *               between them.
	  *               Accepts a surrounding (compared) pixel as the first parameter and the central (targeted)
	  *               pixel as the second value.
	  * @param createEdgePixel A function that accepts the color of a surrounding pixel and the color of the
	  *                        central (targeted) pixel, and returns the new color that will be given to the central
	  *                        pixel.
	  *                        Only called in situations where 'isEdge' has returned true for that pixel pair.
	  *                        The surrounding pixel is either selected by taking the first surrounding pixel that
	  *                        qualifies the 'isEdge' condition, or by reducing the surrounding pixels by finding
	  *                        the maximum using 'reduceOrdering', if specified.
	  * @return Copy of this image with altered edges.
	  *         If 'onlyEdges' was set to true, non-edge pixels have been removed.
	  */
	def highlightEdgesWith(reduceOrdering: Option[Ordering[Color]] = None, strokeWidth: Int = 1,
	                       onlyEdges: Boolean = false)
	                      (isEdge: (Color, Color) => Boolean)
	                      (createEdgePixel: (Color, Color) => Color) =
		mapPixels { pixels =>
			Pixels(Matrix.fill(pixels.size) { p =>
				val original = pixels(p)
				// Finds the pixels around the specified point that form edges with the specified point
				val edgePixelsIter = pixels.indicesAroundIterator(p, range = strokeWidth, onlyOrthogonal = true)
					.map { pixels(_) }
				// Produces the "edge pixel" (representative) either by reducing all available edge pixels to one,
				// or by selecting the first match
				val edgePixel = reduceOrdering match {
					case Some(ordering) =>
						Use(ordering) { implicit ord => edgePixelsIter.filter { isEdge(_, original) }.maxOption }
					case None => edgePixelsIter.find { isEdge(_, original) }
				}
				// Converts the edge into a new pixel color to assign
				edgePixel.map { createEdgePixel(_, original) }
					// Assigns the pixel color or possibly removes the original pixel
					.getOrElse { if (onlyEdges) original.withAlpha(0.0) else original }
			})
		}
	
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
	def withColorOverlay(hue: Color) = mapEachPixel { c => hue.withAlpha(c.alpha) }
	/**
	  * @param color Background color to use
	  * @return This image with a painted background
	  */
	def withBackground(color: Color) = {
		// Case: Has positive size => Paints this image over a solid background
		if (sourceResolution.sign.isPositive && color.alpha > 0)
			Image.paint(sourceResolution) { drawer =>
				drawer.draw(Bounds(Point.origin, sourceResolution))(DrawSettings.onlyFill(color))
				drawWith(drawer, sourceResolutionOrigin)
			}.copy(scaling = scaling, specifiedOrigin = specifiedOrigin)
		// Case: Zero-sized image => No need to paint
		else
			this
	}
	
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
				if (newSize.isNegativeOrZero)
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
			toAwt match {
				case Some(buffer) =>
					// Draws the other image on the buffer
					// Determines the scaling to apply, if necessary
					Drawer(buffer.createGraphics()).consume { d =>
						(overlayImage / scaling).drawWith(d.withClip(Bounds(Point.origin, sourceResolution)),
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
	def paintedOver[U](paint: Drawer => U) = {
		toAwt match {
			case Some(buffer) =>
				// Paints into created buffer
				Drawer(buffer.createGraphics())
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
		else {
			// Creates the new buffer image
			val buffer = new BufferedImage(targetSize.width.round.toInt, targetSize.height.round.toInt,
				BufferedImage.TYPE_INT_ARGB)
			// Draws this image to the center of the image
			val topLeftPosition = (targetSize - size).toPoint / 2
			val originDrawPosition = topLeftPosition + origin
			Drawer(buffer.createGraphics()).consume { drawer => drawWith(drawer, originDrawPosition) }
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
			Drawer(buffer.createGraphics()).consume { d =>
				drawWith(d, transformedOrigin - transformedBounds.topLeft, Some(transformation))
			}
			// Wraps the image
			Image(buffer, origin = if (specifiesOrigin) Some(transformedOrigin - transformedBounds.topLeft) else None)
		}
	}
	
	/**
	  * Converts this image into a Base 64 encoded string
	  * @param initialBufferSize Initial size of the internal buffer used
	  * @param imageFormatName Informal name of the resulting image format. Default = "png".
	  * @return A Base 64 encoded string based on this image's contents.
	  *         A failure if image-writing failed.
	  *         Contains an empty string if this image was empty.
	  */
	def encodeToBase64(initialBufferSize: Int = 1024, imageFormatName: String = "png") = {
		source match {
			case Some(image) =>
				// Opens an output stream for writing this image into it
				new ByteArrayOutputStream(initialBufferSize).tryConsume { outputStream =>
					// Writes this image into the stream
					ImageIO.write(image, imageFormatName, outputStream)
					
					// Reads the stream as a Base 64 encoded string
					Base64.getEncoder.encodeToString(outputStream.toByteArray)
				}
			
			// Case: Empty image => Yields an empty string
			case None => Success("")
		}
	}
	
	/**
	 * Writes this image to a file
	 * @param filePath Path where the file will be written
	 * @return Success or failure. Contains false if this image was empty and therefore not written.
	 */
	def writeToFile(filePath: Path) = filePath.createParentDirectories().flatMap { path =>
		_write { ImageIO.write(_, path.fileType, path.toFile) }
	}
	
	/**
	  * Writes this image into an output stream.
	  * Note: This doesn't close the specified stream, naturally.
	  * @param stream Stream to write this image to
	  * @param imageFormatName Name of the informal image format assigned. Default = "png".
	  * @return Success or failure. Contains false if this image was empty and therefore not written.
	  */
	def writeToStream(stream: OutputStream, imageFormatName: String = "png") =
		_write { ImageIO.write(_, imageFormatName, stream) }
	
	// Handles the empty image use-case as well as write function result-handling
	private def _write(write: BufferedImage => Boolean) = source match {
		case Some(source) =>
			Try { write(source) }.flatMap { writerFound =>
				if (writerFound)
					Success(true)
				else
					Failure(new IOException("No suitable writer found for writing this image"))
			}
		case None => Success(false)
	}
}