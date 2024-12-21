package utopia.genesis.image

import utopia.flow.collection.immutable.{Matrix, Pair}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.parse.AutoClose._
import utopia.flow.util.{Mutate, Use}
import utopia.genesis.graphics.{Drawer, StrokeSettings}
import utopia.genesis.image.transform.{Blur, HueAdjust, IncreaseContrast, Invert, Sharpen, Threshold}
import utopia.paradigm.angular.{Angle, DirectionalRotation}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.enumeration.{Alignment, Direction2D}
import utopia.paradigm.shape.shape1d.Dimension
import utopia.paradigm.shape.shape1d.vector.Vector1D
import utopia.paradigm.shape.shape2d._
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.{Size, Sized}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.transform.{Adjustment, LinearSizeAdjustable}

import java.awt.image.{BufferedImage, BufferedImageOp}

/**
  * Common trait for immutable image representations
  * @tparam Repr The implementing (concrete) image class
  * @author Mikko Hilpinen
  * @since 20.12.2024
  */
trait ImageLike[+Repr] extends ImageView with LinearSizeAdjustable[Repr] with Sized[Repr] with MaybeEmpty[Repr]
{
	// ABSTRACT --------------------
	
	/**
	  * @return An empty (0 sized) copy of this image
	  */
	def empty: Repr
	
	/**
	  * @return A buffered image copied from the source data of this image. None if this image is empty.
	  */
	def toAwt: Option[BufferedImage]
	
	// TODO: Possibly rename some of these
	/**
	  * @return A copy of this image that isn't scaled above 100%
	  */
	def downscaled: Repr
	/**
	  * @return A copy of this image that isn't scaled below 100%
	  */
	def upscaled: Repr
	/**
	  * @return Copy of this image scaled to match the source resolution.
	  *         If multiple source resolutions are available, selects the largest option.
	  */
	def fullSized: Repr
	
	/**
	  * If this image is downscaled, lowers the source image resolution to match the current size of this image.
	  * Will not affect non-scaled or upscaled images. Please note that <b>this operation cannot be reversed</b>
	  * @return A copy of this image with (possibly) lowered source resolution
	  */
	def withMinimumResolution: Repr
	
	/**
	  * @return A copy of this image without a specified origin location
	  */
	def withoutSpecifiedOrigin: Repr
	
	/**
	  * @return A copy of this image that contains minimum amount of pixels.
	  *         I.e. empty (invisible) rows and columns are removed from the edges.
	  *         The origin of this image is preserved, if defined.
	  */
	def cropped: Repr
	
	/**
	  * @param newOrigin A new image origin <b>relative to the current image size</b>, which is scaling-dependent
	  * @return A copy of this image with the specified origin
	  */
	def withOrigin(newOrigin: Point): Repr
	/**
	  * @param f A mapping function for applied image origin.
	  *          Accepts two parameters:
	  *             1. This image's current origin
	  *             1. Size of this image relative to the specified origin.
	  *
	  *          Yields a modified origin in the same coordinate system / space as the specified origin.
	  *
	  * @return A copy of this image with mapped origin
	  */
	def mapOrigin(f: (Point, Size) => Point): Repr
	
	/**
	  * Scales this image
	  * @param scaling The scaling factor
	  * @return A scaled version of this image
	  */
	def *(scaling: HasDoubleDimensions): Repr
	/**
	  * Downscales this image
	  * @param divider The dividing factor
	  * @return A downscaled version of this image
	  */
	def /(divider: HasDoubleDimensions): Repr
	
	/**
	  * Creates a copy of this image where the source data is limited to a certain resolution. The use size and the
	  * aspect ratio of this image are preserved, however.
	  * @param maxResolution The maximum resolution allowed for this image
	  * @return A copy of this image with equal or smaller resolution than that specified
	  */
	def withMaxSourceResolution(maxResolution: Size): Repr
	
	/**
	  * Creates a copy of this image with adjusted alpha value (transparency)
	  * @param newAlpha The new alpha value for this image [0, 1]
	  * @return A new image
	  */
	def withAlpha(newAlpha: Double): Repr
	
	/**
	  * Takes a sub-image from this image (meaning only a portion of this image)
	  * @param area The relative area that is cut from this image. The (0,0) is considered to be in the top left
	  *             corner of this image.
	  * @return The portion of this image within the relative area
	  */
	def subImage(area: Bounds): Repr
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
	def withCanvasSize(canvasSize: Size, alignment: Alignment = Center): Repr
	
	/**
	  * Transforms this image on the pixels -level
	  * @param preserveShade Whether image shade calculation may be preserved (default = false)
	  * @param pixelsTransform A transform function applied to the pixels of this image
	  * @param originTransform A transform function applied to the (explicitly specified) origin of this image.
	  *                        Accepts the current origin, as well as the size of this image
	  *                        relative to that origin coordinate.
	  *                        Yields the new origin in that same coordinate space.
	  * @return Transformed copy of this image
	  */
	def mutatePixels(preserveShade: Boolean = false)
	                (pixelsTransform: Mutate[Pixels])(originTransform: (Point, Size) => Point): Repr
	/**
	  * @param area The mapped relative area
	  * @param f A function that maps pixel colors
	  * @return A copy of this image with pixels mapped within the target area
	  */
	def mapArea(area: Area2D)(f: Color => Color): Repr
	
	/**
	  * @param paint A function for painting over this image.
	  *              Accepts:
	  *                 1. A drawer that is clipped to this image's area
	  *                 1. Size of the drawable area within this image
	  *
	  *              During drawing, (0,0) is in the top-left corner of this image and
	  *              the bottom-right corner matches the specified size parameter.
	  *
	  * @return A copy of this image with the paint operation applied
	  */
	def paintedOver[U](paint: (Drawer, Size) => U): Repr
	
	/**
	  * @param overlayImage An image that will be drawn on top of this image
	  * @param overlayPosition A position that determines where the origin of the other image will be placed
	  * @param relativeToOrigin Whether the specified 'overlayPosition' is relative to this image's origin,
	  *                         instead of the top-left corner.
	  *                         Default = true = 'overlayPosition' is relative to this image's origin
	  * @return A new image where the specified image is drawn on top of this one
	  */
	def withOverlay(overlayImage: Image, overlayPosition: Point = Point.origin, relativeToOrigin: Boolean = true): Repr
	/**
	  * @param color Background color to use
	  * @return This image with a painted background
	  */
	def withBackground(color: Color): Repr
	
	/**
	  * Applies a bufferedImageOp to this image, producing a new image
	  * @param op The operation that is applied
	  * @return A new image with operation applied
	  */
	def filterWith(op: BufferedImageOp): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return A copy of this image where the origin is placed at the center of the image
	  */
	def withCenterOrigin = withOrigin((size / 2).toPoint)
	
	/**
	  * @return A copy of this image where x-axis is reversed
	  */
	def flippedHorizontally =
		mutatePixels(preserveShade = true) { _.flippedHorizontally } { (origin, size) => origin.mapX { size.width - _ } }
	/**
	  * @return A copy of this image where y-axis is reversed
	  */
	def flippedVertically =
		mutatePixels(preserveShade = true) { _.flippedVertically } { (origin, size) => origin.mapY { size.height - _ } }
	
	/**
	  * @return A copy of this image with increased contrast
	  */
	def withIncreasedContrast = mapEachPixel(IncreaseContrast.apply)
	/**
	  * @return A copy of this image with inverted colors
	  */
	def inverted = mapEachPixel(Invert.apply)
	/**
	  * @return A blurred copy of this image
	  */
	def blurred = blurredBy(1)
	/**
	  * @return A sharpened copy of this image
	  */
	def sharpened = sharpenedBy(5)
	
	
	// IMPLEMENTED	----------------
	
	override def nonEmpty = !isEmpty
	
	override def croppedToFitWithin(maxArea: HasDoubleDimensions) = {
		if (fitsWithin(maxArea))
			self
		else {
			val requiredCropping = size - maxArea
			crop(Insets.symmetric(requiredCropping))
		}
	}
	override def croppedToFitWithin(maxLength: Dimension[Double]) = {
		if (fitsWithin(maxLength))
			self
		else {
			val requiredCropping = lengthAlong(maxLength.axis) - maxLength.value
			crop(Insets.symmetric(Vector1D(requiredCropping, maxLength.axis)))
		}
	}
	
	
	// OTHER	----------------
	
	/**
	 * @param other Another image
	 * @return A copy of this image with the other image drawn on top of this one with its origin at the point
	  *         of this image's origin (if specified)
	 */
	def +(other: Image) = withOverlay(other)
	
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
	  * @param translation Translation applied to current (scaled) image origin
	  * @return A copy of this image with translated origin
	  */
	def withTranslatedOrigin(translation: HasDoubleDimensions) = withOrigin(origin + translation)
	
	/**
	  * Crops this image from the sides
	  * @param insets Insets to crop out of this image
	  * @return A cropped copy of this image
	  */
	def crop(insets: Insets) = {
		val positiveInsets = insets.positive
		val totalInsets = positiveInsets.total
		if (totalInsets.existsDimensionWith(size) { _ >= _ })
			empty
		else
			subImage(Bounds(Point.origin, size) - positiveInsets)
	}
	/**
	  * @param side Side from which to crop from this image
	  * @param amount Amount of pixels to crop from this image
	  * @return A cropped copy of this image
	  */
	def cropFromSide(side: Direction2D, amount: Double) = crop(Insets.towards(side, amount))
	
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
	  * @param f A mapping function for pixel tables
	  * @return A copy of this image with mapped pixels
	  */
	def mapPixels(f: Mutate[Pixels]): Repr = mutatePixels()(f) { (o, _) => o }
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
	  * Creates a blurred copy of this image
	  * @param intensity The blurring intensity [0, 1], defaults to 1
	  * @return A blurred version of this image
	  */
	def blurredBy(intensity: Double) = filterWith(Blur(intensity).op)
	/**
	  * Creates a sharpened copy of this image
	  * @param intensity The sharpening intensity (default = 5)
	  * @return A sharpened copy of this image
	  */
	def sharpenedBy(intensity: Double) = filterWith(Sharpen(intensity).op)
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
	def withAdjustedHue(sourceHue: Angle, sourceRange: Angle, targetHue: Angle) = {
		val adjust = HueAdjust(sourceHue, sourceRange, targetHue)
		mapEachPixel(adjust.apply)
	}
	/**
	  * Creates a copy of this image where each color channel is limited to a certain number of values
	  * @param colorAmount The number of possible values for each color channel
	  * @return A copy of this image with limited color options
	  */
	def withThreshold(colorAmount: Int) = {
		val threshold = Threshold(colorAmount)
		mapEachPixel(threshold.apply)
	}
	
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
	 * @param hue Hue for every pixel in this image
	 * @return A new image with all pixels set to provided color. Original alpha channel is preserved, however.
	 */
	def withColorOverlay(hue: Color) = mapEachPixel { c => hue.withAlpha(c.alpha) }
	
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
			toConcreteImage
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
			toConcreteImage
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
}