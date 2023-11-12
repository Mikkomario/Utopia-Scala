package utopia.genesis.image

import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.AutoClose._
import utopia.flow.view.mutable.caching.MutableLazy
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.image.transform._
import utopia.paradigm.angular.{Angle, DirectionalRotation, Rotation}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import java.awt.image.{BufferedImage, BufferedImageOp}

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
		implicit val ds: DrawSettings = DrawSettings.onlyFill(background)
		val source = new BufferedImage(size.width.round.toInt, size.height.round.toInt, BufferedImage.TYPE_INT_ARGB)
		Drawer(source.createGraphics()).consume { _.draw(Bounds(Point.origin, size)) }
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
	
	// TODO: Use a mutable pixel matrix
	private val pixelsCache = MutableLazy {
		source match {
			case Some(image) => Pixels.fromBufferedImage(image, lazily = true)
			case None => Pixels.empty
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
	def *=(mod: HasDoubleDimensions) = scaling *= mod
	
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
	def resizeToFill(area: Size) = if (size.nonZero) *=((area / size).xyPair.max)
	
	/**
	  * Resizes this image to exactly fit the specified area. Preserves shape, though, which may cause this image to
	  * shrink inside the area along one axis
	  * @param area Area to fit to
	  */
	def resizeToFit(area: Size) = if (size.nonZero) *=((area / size).xyPair.min)
	
	/**
	  * Makes sure this image fills the specified area. If this image is already larger than the area, does nothing
	  * @param area Area to fill
	  */
	def expandToFill(area: Size) = if (!area.fitsWithin(size)) resizeToFill(area)
	
	/**
	  * Makes sure this image fits into the specified area. If this image is already smaller than the area, does nothing
	  * @param area Area to fit into
	  */
	def shrinkToFit(area: Size) = if (!size.fitsWithin(area)) resizeToFit(area)
	
	/**
	  * Places a limitation upon either image width or height. Preserves shape.
	  * @param side Targeted axis
	  * @param maxLength Length limitation for that axis
	  */
	def limitAlong(side: Axis2D, maxLength: Double) =
		if (size(side) > maxLength) shrinkToFit(size.withDimension(side(maxLength)))
	
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
	  * Maps the pixels of this image
	  * @param f A function for mapping all pixels at once
	  */
	def updatePixels(f: Pixels => Pixels) = {
		if (source.isDefined) {
			val newPixels = f(pixels)
			_source = Some(newPixels.toBufferedImage)
			pixelsCache.value = newPixels
		}
	}
	/**
	  * Maps the pixel table of this image
	  * @param f A function for mapping a pixel table
	  */
	@deprecated("Please use .updatePixels(...) instead", "v3.2")
	def updatePixelTable(f: Pixels => Pixels) = updatePixels(f)
	
	/**
	  * Maps all of the pixels in this image
	  * @param f A function for mapping pixel colors
	  */
	def updateEachPixel(f: Color => Color) = updatePixels { _.map(f) }
	/**
	  * Maps all of the pixels in this image
	  * @param f A function for mapping pixel colors, also accepts pixel location (in source resolution context)
	  */
	def updatePixelsWithIndex(f: (Color, Pair[Int]) => Color) = updatePixels { _.mapWithIndex(f) }
	/**
	  * Maps all of the pixels in this image
	  * @param f A function for mapping pixel colors, also accepts pixel location (in source resolution context)
	  */
	def updatePixelPoints(f: (Color, Point) => Color) = updatePixels { _.mapPoints(f) }
	
	/**
	  * Maps pixels in this image in a specified area
	  * @param area Targeted area inside this image (in scaled context), where (0,0) is at the top-left corner
	  *             of this image
	  * @param f A mapping function for pixel colors
	  */
	def updateArea(area: Area2D)(f: Color => Color) = updatePixelPoints { (c, p) =>
		if (area.contains(p * scaling)) f(c) else c
	}
	
	/**
	  * Updates the size of this image
	  * @param newSize New size for this image
	  * @param preserveShape Whether image shape should be preserved (default = true)
	  */
	def resize(newSize: Size, preserveShape: Boolean = true) = {
		if (preserveShape)
			*=((newSize.width / width) min (newSize.height / height))
		else
			*=(newSize / size)
	}
	
	/**
	  * Flips this image horizontally
	  */
	def flipHorizontally() = {
		updatePixels { _.flippedHorizontally }
		updateSpecifiedOrigin { _.map { o => Point(sourceResolution.width - o.x, o.y) } }
	}
	/**
	  * Flips this image vertically
	  */
	def flipVertically() = {
		updatePixels { _.flippedVertically }
		updateSpecifiedOrigin { _.map { o => Point(o.x, sourceResolution.height - o.y) } }
	}
	
	/**
	  * Applies a color overlay on this image
	  * @param hue Color to use for all pixels
	  */
	def addColorOverlay(hue: Color) = updateEachPixel { c => hue.withAlpha(c.alpha) }
	
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
	def adjustHue(amount: DirectionalRotation) = updateEachPixel { _ + amount }
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
	def overlay(image: Image, overlayPosition: Point = Point.origin) = {
		if (image.nonEmpty)
			source match {
				case Some(target) =>
					// Draws the overlay image on the source directly
					// Calculates applied scaling
					Drawer(target.createGraphics()).consume { d =>
						(image / scaling).drawWith(d.withClip(Bounds(Point.origin, sourceResolution)),
							sourceResolutionOrigin + overlayPosition)
					}
				case None =>
					// Overwrites the source with the new image
					_source = image.toAwt
					pixelsCache.value = image.pixels
			}
	}
	
	/**
	  * Applies a paint function over this image
	  * @param paint A function that will paint over this image. The provided drawer is clipped to this image's area.
	  */
	def paintOver[U](paint: Drawer => U) =
		source.foreach { target => Drawer(target.createGraphics()).consume(paint) }
}
