package utopia.genesis.image

import utopia.flow.operator.equality.EqualsBy
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.caching.{Lazy, PreInitializedLazy}
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.paradigm.color.ColorShade.Dark
import utopia.paradigm.color.{Color, ColorShade}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import java.awt.image.{BufferedImage, BufferedImageOp}
import java.io.{ByteArrayOutputStream, IOException, OutputStream}
import java.nio.file.Path
import java.util.Base64
import javax.imageio.ImageIO
import scala.util.{Failure, Success, Try}

object ConcreteImage extends ImageFactory[ConcreteImage]
{
	// ATTRIBUTES   -----------------
	
	override val empty = new ConcreteImage(None, Vector2D.identity, 1.0, None, Lazy.initialized(Pixels.empty),
		Lazy.initialized(Dark))
	
	
	// IMPLEMENTED  -----------------
	
	override def apply(image: BufferedImage, scaling: Vector2D, alpha: Double, origin: Option[Point]) = {
		val lazyPixels = Lazy { Pixels.fromBufferedImage(image, lazily = true) }
		new ConcreteImage(Some(image), scaling, alpha, origin, lazyPixels, lazyPixels.map { _.averageShade })
	}
	
	override def fromPixels(pixels: Pixels) = pixels.notEmpty match {
		case Some(pixels) =>
			new ConcreteImage(Some(pixels.toBufferedImage), Vector2D.identity, 1.0, None, Lazy.initialized(pixels),
				Lazy { pixels.averageShade })
		case None => empty
	}
}

/**
  * This is a wrapper for the buffered image class
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1
  */
class ConcreteImage private(override protected val source: Option[BufferedImage], override val scaling: Vector2D,
                            override val alpha: Double, override val specifiedOrigin: Option[Point],
                            private val _pixels: Lazy[Pixels], private val _shade: Lazy[ColorShade])
	extends ConcreteImageView with Image with ImageLike[ConcreteImage] with EqualsBy
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
	  * @return A copy of this image with original (100%) scaling
	  */
	def withOriginalSize = fullSized
	
	/**
	  * @return A Base 64 encoded string based on this image's contents.
	  *         A failure if image-writing failed.
	  *         Contains an empty string if this image was empty.
	  */
	def toBase64String = encodeToBase64()
	
	
	// IMPLEMENTED	----------------
	
	override def self = this
	override def empty: ConcreteImage = Image.empty
	override def toConcreteImage: ConcreteImage = this
	
	override def isEmpty = source.isEmpty || size.dimensions.exists { _ < 1.0 }
	
	override protected def equalsProperties: Seq[Any] = Vector(scaling, alpha, specifiedOrigin, source)
	
	override def pixels = _pixels.value
	override def shade: ColorShade = _shade.value
	
	override def toAwt = withMinimumResolution.source.map { img =>
		val colorModel = img.getColorModel
		val isAlphaPremultiplied = colorModel.isAlphaPremultiplied
		val raster = img.copyData(null)
		new BufferedImage(colorModel, raster, isAlphaPremultiplied, null)
			.getSubimage(0, 0, img.getWidth, img.getHeight)
	}
	
	override def downscaled = if (scaling.dimensions.exists { _ > 1 }) withScaling(scaling.map { _ min 1 }) else this
	override def upscaled = if (scaling.dimensions.exists { _ < 1 }) withScaling(scaling.map { _ max 1 }) else this
	override def withMinimumResolution = {
		if (scaling.dimensions.forall { _ >= 1 })
			this
		else
			withSourceResolution(size topLeft sourceResolution, preserveUseSize = true)
	}
	override def fullSized: ConcreteImage = if (scaling == Vector2D.identity) this else withScaling(1)
	
	override def withoutSpecifiedOrigin = if (specifiesOrigin) copy(origin = None) else this
	override def withCenterOrigin = withSourceResolutionOrigin((sourceResolution / 2).toPoint)
	
	override def cropped = {
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
			case None => empty
		}
	}
	
	override def *(scaling: Double): ConcreteImage = withScaling(this.scaling * scaling)
	override def *(scaling: HasDoubleDimensions): ConcreteImage = withScaling(this.scaling * scaling)
	override def /(divider: HasDoubleDimensions): ConcreteImage = withScaling(scaling / divider)
	
	override def withOrigin(newOrigin: Point) = withSourceResolutionOrigin(newOrigin / scaling)
	override def mapOrigin(f: (Point, Size) => Point): ConcreteImage =
		withSourceResolutionOrigin(f(sourceResolutionOrigin, sourceResolution))
	
	override def withSize(size: Size) = this * (size / this.size)
	
	override def withMaxSourceResolution(maxResolution: Size) = {
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
	
	override def withAlpha(newAlpha: Double) = copy(alpha = newAlpha max 0 min 1)
	
	override def subImage(area: Bounds) = source match {
		case Some(source) =>
			area.overlapWith(Bounds(Point.origin, size)) match {
				case Some(overlap) => _subImage(source, overlap / scaling)
				case None =>
					Image(new BufferedImage(0, 0, source.getType), scaling, alpha,
						specifiedOrigin.map { _ - area.position / scaling })
			}
		case None => this
	}
	
	override def withCanvasSize(canvasSize: Size, alignment: Alignment = Center) = {
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
						modified.copy(lazyShade = _shade)
					// Case: Cropping plus padding at the same time
					// => We don't know the resulting shade without calculating it
					else
						modified
				
				// Case: Empty image (no awt source) => No need to paint anything
				case None => withSize(canvasSize)
			}
	}
	
	override def mutatePixels(preserveShade: Boolean)
	                         (pixelsTransform: Mutate[Pixels])(originTransform: (Point, Size) => Point) =
	{
		if (source.isDefined) {
			val newPixels = pixelsTransform(pixels)
			new ConcreteImage(Some(newPixels.toBufferedImage), scaling, alpha,
				specifiedOrigin.map { originTransform(_, sourceResolution) }, PreInitializedLazy(newPixels),
				if (preserveShade && _shade.isInitialized) _shade else Lazy { newPixels.averageShade })
		}
		else
			this
	}
	override def mapArea(area: Area2D)(f: Color => Color) =
		mapPixelPoints { (c, p) => if (area.contains(p * scaling)) f(c) else c }
	
	override def withOverlay(overlayImage: Image, overlayPosition: Point, relativeToOrigin: Boolean) = {
		// If one of the images is empty, uses the other image
		if (overlayImage.isEmpty)
			this
		else
			toAwt match {
				case Some(buffer) =>
					// Draws the other image on the buffer
					// Determines the scaling to apply, if necessary
					val drawOrigin = if (relativeToOrigin) sourceResolutionOrigin else Point.origin
					Drawer(buffer.createGraphics()).consume { d =>
						(overlayImage / scaling)
							.drawWith(d.withClip(Bounds(Point.origin, sourceResolution)), drawOrigin + overlayPosition)
					}
					// Wraps the result
					Image(buffer, scaling, alpha, specifiedOrigin)
				
				case None => overlayImage.toConcreteImage
			}
	}
	override def withBackground(color: Color) = {
		// Case: Has positive size => Paints this image over a solid background
		if (sourceResolution.sign.isPositive && color.alpha > 0)
			ConcreteImage.paint(sourceResolution) { drawer =>
				drawer.draw(Bounds(Point.origin, sourceResolution))(DrawSettings.onlyFill(color))
				drawWith(drawer, sourceResolutionOrigin)
			}.copy(scaling = scaling, origin = specifiedOrigin)
		// Case: Zero-sized image => No need to paint
		else
			this
	}
	
	override def paintedOver[U](paint: (Drawer, Size) => U): ConcreteImage = toAwt match {
		case Some(buffer) =>
			// Paints into created buffer
			Drawer(buffer.createGraphics())
				.consume { d => paint(d.withClip(Bounds(Point.origin, sourceResolution)), sourceResolution) }
			Image(buffer, scaling, alpha, specifiedOrigin)
		
		case None => this
	}
	
	override def filterWith(op: BufferedImageOp) = source match {
		case Some(source) =>
			val destination = new BufferedImage(source.getWidth, source.getHeight, source.getType)
			op.filter(source, destination)
			Image(destination, scaling, alpha, specifiedOrigin)
		case None => this
	}
	
	
	// OTHER	----------------
	
	/**
	  * @param newOrigin A new image origin relative to the top left coordinate in the source resolution image
	  * @return A copy of this image with the specified origin
	  */
	def withSourceResolutionOrigin(newOrigin: Point) = copy(origin = Some(newOrigin))
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
	  * @param scaling A scaling modifier applied to the original image
	  * @return A scaled version of this image
	  */
	def withScaling(scaling: HasDoubleDimensions): ConcreteImage = copy(scaling = Vector2D.from(scaling))
	/**
	  * @param scaling A scaling modifier applied to the original image
	  * @return A scaled version of this image
	  */
	def withScaling(scaling: Double): ConcreteImage = withScaling(Vector2D(scaling, scaling))
	
	/**
	  * @param area Target area (minimum)
	  * @return A copy of this image that is larger or equal to the target area. Shape is preserved.
	  */
	@deprecated("Please use filling(Dimensional) instead", "v3.1")
	def largerThan(area: Size) = filling(area)
	
	/**
	  * Converts this one image into a strip containing multiple parts. The splitting is done horizontally.
	  * @param numberOfParts The number of separate parts within this image
	  * @param marginBetweenParts The horizontal margin between the parts within this image in pixels (default = 0)
	  * @return A strip containing numberOfParts images, which all are sub-images of this image
	  */
	def split(numberOfParts: Int, marginBetweenParts: Int = 0): Strip = {
		val subImageWidth = (width - marginBetweenParts * (numberOfParts - 1)) / numberOfParts
		val subImageSize = Size(subImageWidth, height)
		Strip((0 until numberOfParts).map {
			index => subImage(Bounds(Point(index * (subImageWidth + marginBetweenParts)), subImageSize)) }.toVector)
	}
	
	/**
	  * @param f A mapping function for pixel tables
	  * @return A copy of this image with mapped pixels
	  */
	@deprecated("Please use .mapPixels instead", "v3.2")
	def mapPixelTable(f: Pixels => Pixels) = mapPixels(f)
	
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
					empty
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
	
	private def copy(source: Option[BufferedImage] = source, scaling: Vector2D = scaling, alpha: Double = alpha,
	                 origin: Option[Point] = specifiedOrigin, lazyShade: Lazy[ColorShade] = _shade) =
		new ConcreteImage(source, scaling, alpha, origin, _pixels, lazyShade)
	
	// Only works when specified area is inside the original image's bounds and scaled according to source resolution
	private def _subImage(img: BufferedImage, relativeArea: Bounds) = {
		val area = relativeArea.round
		val newSource = img.getSubimage(area.leftX.toInt, area.topY.toInt, area.width.toInt, area.height.toInt)
		val newLazyPixels = Lazy { _pixels.value.view(area) }
		new ConcreteImage(Some(newSource), scaling, alpha, specifiedOrigin.map { _ - area.position },
			newLazyPixels, newLazyPixels.map { _.averageShade })
	}
	
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