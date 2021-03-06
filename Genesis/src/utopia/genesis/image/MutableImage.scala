package utopia.genesis.image

import java.awt.image.{BufferedImage, BufferedImageOp}

import utopia.flow.datastructure.mutable.MutableLazy
import utopia.genesis.color.Color
import utopia.genesis.image.transform.{Blur, HueAdjust, ImageTransform, IncreaseContrast, Invert, Sharpen, Threshold}
import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.{Angle, Rotation}
import utopia.genesis.shape.shape2D.{Area2D, Bounds, Point, Size, Vector2D}
import utopia.genesis.shape.template.Dimensional
import utopia.genesis.util.Drawer

object MutableImage
{
	/**
	  * Creates a new empty image
	  * @param size The size of this image (in pixels)
	  * @param background Background color for this image (default = fully transparent black)
	  * @return A new image
	  */
	def canvas(size: Size, background: Color = Color.transparentBlack) =
	{
		val source = new BufferedImage(size.width.round.toInt, size.height.round.toInt, BufferedImage.TYPE_INT_ARGB)
		Drawer.use(source.createGraphics()) { _.onlyFill(background).draw(Bounds(Point.origin, size)) }
		new MutableImage(Some(source))
	}
}

/**
  * A mutable image implementation that allows direct modifications
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2.4
  */
class MutableImage(initialSource: Option[BufferedImage], initialScaling: Vector2D = Vector2D.identity,
				   initialAlpha: Double = 1.0, initialOrigin: Option[Point] = None) extends ImageLike
{
	// ATTRIBUTES	-------------------------------
	
	private var _source = initialSource
	
	private val pixelsCache = MutableLazy {
		source match
		{
			case Some(image) => PixelTable.fromBufferedImage(image)
			case None => PixelTable.empty
		}
	}
	
	private var _alpha = (initialAlpha max 0.0) min 1.0
	
	var scaling = initialScaling
	var specifiedOrigin = initialOrigin
	def specifiedOrigin_=(newOrigin: Point): Unit = specifiedOrigin = Some(newOrigin)
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return An immutable copy of this image's current state
	  */
	def immutableCopy = source match
	{
		case Some(img) =>
			val colorModel = img.getColorModel
			val isAlphaPremultiplied = colorModel.isAlphaPremultiplied
			val raster = img.copyData(null)
			Image(new BufferedImage(colorModel, raster, isAlphaPremultiplied, null)
				.getSubimage(0, 0, img.getWidth, img.getHeight), scaling, alpha, specifiedOrigin)
		case None => Image.empty
	}
	
	def sourceResolutionOrigin_=(newOrigin: Point) = specifiedOrigin = newOrigin
	
	def origin_=(newOrigin: Point) = specifiedOrigin = newOrigin / scaling
	
	
	// IMPLEMENTED	-------------------------------
	
	override def source = _source
	
	override def alpha = _alpha
	def alpha_=(newAlpha: Double) = _alpha = (newAlpha max 0.0) min 1.0
	
	override def sourceResolution = source match
	{
		case Some(s) => Size(s.getWidth, s.getHeight)
		case None => Size.zero
	}
	
	override def size = sourceResolution * scaling
	def size_=(newSize: Size) = resize(newSize, preserveShape = false)
	
	override def bounds = Bounds(-origin, size)
	
	override def isEmpty = source.isEmpty
	
	override def pixels = pixelsCache.value
	
	override def preCalculatedPixels = pixelsCache.current
	
	
	// OTHER	----------------------------------
	
	/**
	  * Scales the size of this image
	  * @param mod Size scaling factor
	  */
	def *=(mod: Double) = scaling *= mod
	
	/**
	  * Scales the size of this image
	  * @param mod Size scaling factor
	  */
	def *=(mod: Dimensional[Double]) = scaling *= mod
	
	/**
	  * Divides the size of this image
	  * @param div Size divider
	  */
	def /=(div: Double) = scaling /= div
	
	/**
	  * Adds an image overlay over this image (overlay image origin will be placed at this image's origin)
	  * @param overlay The overlay image
	  */
	def +=(overlay: Image) = this.overlay(overlay)
	
	/**
	  * Maps the alpha value of this image
	  * @param f A mapping function for image alpha (opacity)
	  */
	def updateAlpha(f: Double => Double) = alpha = f(alpha)
	
	/**
	  * Maps the specified origin of this image
	  * @param f A mapping function for specified origin in source resolution. Uses None when origin is not specified.
	  */
	def updateSpecifiedOrigin(f: Option[Point] => Option[Point]) = specifiedOrigin = f(specifiedOrigin)
	
	/**
	  * Maps the origin of this image in source resolution context
	  * @param f A mapping function for this image's source resolution origin
	  */
	def updateSourceResolutionOrigin(f: Point => Point) = sourceResolutionOrigin = f(sourceResolutionOrigin)
	
	/**
	  * Maps the origin of this image in scaled context
	  * @param f A mapping function for this image's origin
	  */
	def updateOrigin(f: Point => Point) = origin = f(origin)
	
	/**
	  * Places the origin of this image to the current image center
	  */
	def centerOrigin() = specifiedOrigin = sourceResolution.toPoint / 2
	
	/**
	  * Downscales this image to 100% scaling or under
	  */
	def downscale() = scaling = scaling.map { _ min 1 }
	
	/**
	  * Upscales this image to 100% scaling or above
	  */
	def upscale() = scaling = scaling.map { _ max 1 }
	
	/**
	  * Removes all scaling, resulting in size equal to source resolution
	  */
	def clearScaling() = scaling = Vector2D.identity
	
	/**
	  * Resizes this image to exactly fill the specified area. Preserves shape, though, which may cause this image to
	  * expand over the area along one axis
	  * @param area Area to fill
	  */
	def resizeToFill(area: Size) = if (size.nonZero) *=((area / size).dimensions2D.max)
	
	/**
	  * Resizes this image to exactly fit the specified area. Preserves shape, though, which may cause this image to
	  * shrink inside the area along one axis
	  * @param area Area to fit to
	  */
	def resizeToFit(area: Size) = if (size.nonZero) *=((area / size).dimensions2D.min)
	
	/**
	  * Makes sure this image fills the specified area. If this image is already larger than the area, does nothing
	  * @param area Area to fill
	  */
	def expandToFill(area: Size) = if (!area.fitsInto(size)) resizeToFill(area)
	
	/**
	  * Makes sure this image fits into the specified area. If this image is already smaller than the area, does nothing
	  * @param area Area to fit into
	  */
	def shrinkToFit(area: Size) = if (!size.fitsInto(area)) resizeToFit(area)
	
	/**
	  * Places a limitation upon either image width or height. Preserves shape.
	  * @param side Targeted axis
	  * @param maxLength Length limitation for that axis
	  */
	def limitAlong(side: Axis2D, maxLength: Double) =
		if (size.along(side) > maxLength) shrinkToFit(size.withDimension(maxLength, side))
	
	/**
	  * Places a width limitation for this image. Preserves shape.
	  * @param maxWidth Maximum allowed width.
	  */
	def limitWidth(maxWidth: Double) = limitAlong(X, maxWidth)
	
	/**
	  * Places a height limitation for this image. Preserves shape.
	  * @param maxHeight Maximum allowed height.
	  */
	def limitHeight(maxHeight: Double) = limitAlong(Y, maxHeight)
	
	/**
	  * Maps the pixel table of this image
	  * @param f A function for mapping a pixel table
	  */
	def updatePixelTable(f: PixelTable => PixelTable) =
	{
		if (source.isDefined)
		{
			val newPixels = f(pixels)
			_source = Some(newPixels.toBufferedImage)
			pixelsCache.value = newPixels
		}
	}
	
	/**
	  * Maps all of the pixels in this image
	  * @param f A function for mapping pixel colors
	  */
	def updatePixels(f: Color => Color) = updatePixelTable { _.map(f) }
	
	/**
	  * Maps all of the pixels in this image
	  * @param f A function for mapping pixel colors, also accepts pixel location (in source resolution context)
	  */
	def updatePixelsWithIndex(f: (Color, Point) => Color) = updatePixelTable { _.mapWithIndex(f) }
	
	/**
	  * Maps pixels in this image in a specified area
	  * @param area Targeted area inside this image (in scaled context), where (0,0) is at the top-left corner
	  *             of this image
	  * @param f A mapping function for pixel colors
	  */
	def updateArea(area: Area2D)(f: Color => Color) = updatePixelsWithIndex { (c, p) =>
		if (area.contains(p * scaling)) f(c) else c
	}
	
	/**
	  * Updates the size of this image
	  * @param newSize New size for this image
	  * @param preserveShape Whether image shape should be preserved (default = true)
	  */
	def resize(newSize: Size, preserveShape: Boolean = true) =
	{
		if (preserveShape)
			*=((newSize.width / width) min (newSize.height / height))
		else
			*=(newSize / size)
	}
	
	/**
	  * Flips this image horizontally
	  */
	def flipHorizontally() =
	{
		updatePixelTable { _.flippedHorizontally }
		updateSpecifiedOrigin { _.map { o => Point(sourceResolution.width - o.x, o.y) } }
	}
	
	/**
	  * Flips this image vertically
	  */
	def flipVertically() =
	{
		updatePixelTable { _.flippedVertically }
		updateSpecifiedOrigin { _.map { o => Point(o.x, sourceResolution.height - o.y) } }
	}
	
	/**
	  * Applies a color overlay on this image
	  * @param hue Color to use for all pixels
	  */
	def addColorOverlay(hue: Color) = updatePixels { c => hue.withAlpha(c.alpha) }
	
	/**
	  * Applies an image transformation to this image
	  * @param transformation A transformation to apply
	  */
	def transformWith(transformation: ImageTransform) = transformation(this)
	
	/**
	  * Increases the contrast in this image
	  */
	def increaseContrast() = transformWith(IncreaseContrast)
	
	/**
	  * Inverts the colors in this image
	  */
	def invert() = transformWith(Invert)
	
	/**
	  * Blurs this image
	  * @param intensity Blur intensity [0, 1] (default = 1)
	  */
	def blur(intensity: Double = 1) = transformWith(Blur(intensity))
	
	/**
	  * Sharpens this image
	  * @param intensity Sharpening intensity (default = 5)
	  */
	def sharpen(intensity: Double = 5.0) = transformWith(Sharpen(intensity))
	
	/**
	  * Adjusts the hue in this image by specified amount
	  * @param amount Rotation to apply to image hue for all pixels
	  */
	def adjustHue(amount: Rotation) = updatePixels { _ + amount }
	
	/**
	  * Adjusts the hue in this image
	  * @param sourceHue Targeted hue
	  * @param sourceRange The width of the targeted area as an angle
	  * @param targetHue The new hue that will replace the targeted hue
	  */
	def adjustHue(sourceHue: Angle, sourceRange: Angle, targetHue: Angle) =
		transformWith(HueAdjust(sourceHue, sourceRange, targetHue))
	
	/**
	  * Transforms this image using a threshold function
	  * @param colorAmount The amount of rgb color variations allowed (for each channel)
	  */
	def threshold(colorAmount: Int) = transformWith(Threshold(colorAmount))
	
	/**
	  * Applies an image operation on this image
	  * @param op The operation to apply
	  */
	def filterWith(op: BufferedImageOp) = source.foreach { source =>
		val destination = new BufferedImage(source.getWidth, source.getHeight, source.getType)
		op.filter(source, destination)
		this._source = Some(destination)
		pixelsCache.reset()
	}
	
	/**
	  * Paints another image over this image
	  * @param image Image to paint over this image
	  * @param overlayPosition The position where the overlay image's origin will be placed
	  *                        (relative to this image's origin). Default = (0,0) = Image origins will overlap.
	  */
	def overlay(image: Image, overlayPosition: Point = Point.origin) =
	{
		if (image.nonEmpty)
			source match
			{
				case Some(target) =>
					// Draws the overlay image on the source directly
					// Calculates applied scaling
					Drawer.use(target.createGraphics()) { d =>
						(image / scaling).drawWith(d.clippedTo(Bounds(Point.origin, sourceResolution)),
							sourceResolutionOrigin + overlayPosition)
					}
				case None =>
					// Overwrites the source with the new image
					_source = image.toAwt
					image.preCalculatedPixels match
					{
						case Some(pixels) => pixelsCache.value = pixels
						case None => pixelsCache.reset()
					}
			}
	}
	
	/**
	  * Applies a paint function over this image
	  * @param paint A function that will paint over this image. The provided drawer is clipped to this image's area.
	  */
	def paintOver(paint: Drawer => Unit) = source.foreach { target => Drawer.use(target.createGraphics())(paint)
		// { d => paint(d.clippedTo(Bounds(Point.origin, sourceResolution))) }
	}
}
