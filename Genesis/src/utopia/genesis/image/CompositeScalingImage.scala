package utopia.genesis.image

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.collection.immutable.range.{NumericSpan, Span}
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.caching.Lazy
import utopia.genesis.graphics.Drawer
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape1d.Dimension
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import java.awt.image.BufferedImageOp

object CompositeScalingImage
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * An empty composite image
	  */
	lazy val empty = new CompositeScalingImage(Empty, Lazy.initialized(Image.empty), Size.zero,
		Pair.twice(NumericSpan.singleValue(0.0)))
		
	
	// OTHER    -----------------------------
	
	/**
	  * Creates a new image by combining multiple different sized images
	  * @param images Images to combine
	  * @return A combined image that displays the best sized image at each situation
	  */
	def apply(images: Seq[Image]) = {
		images.emptyOneOrMany match {
			case None => empty
			case Some(Left(only)) => from(only)
			case Some(Right(images)) =>
				val variants = images
					.map { img =>
						val full = img.fullSized
						full.size -> Lazy.initialized(full)
					}
					.sortBy { _._1.maxDimension }
				val (size, img) = variants.last
				val smallerSize = variants(variants.size - 2)._1
				
				new CompositeScalingImage(variants, img, size,
					smallerSize.xyPair.mergeWith(size.xyPair) { NumericSpan(_, _) })
		}
	}
	/**
	  * Creates a new image by combining multiple different sized images
	  * @return A combined image that displays the best sized image at each situation
	  */
	def apply(first: Image, second: Image, more: Image*): CompositeScalingImage =
		apply(Pair(first, second) ++ more)
	
	/**
	  * @param image An image
	  * @return The specified image as a composite image
	  */
	def from(image: Image) = image match {
		case c: CompositeScalingImage => c
		case i =>
			val maxSize = i.maxScaling match {
				case Some(maxScaling) => i.size * maxScaling
				case None => i.size
			}
			val lazyImage = Lazy.initialized(i)
			new CompositeScalingImage(Single(maxSize -> lazyImage), lazyImage, i.size,
				maxSize.xyPair.map { NumericSpan(0.0, _) })
	}
	
	private def _apply(variants: Seq[(Size, Lazy[Image])], size: Size) =
		variants.emptyOneOrMany match {
			// Case: No images => Displays an empty image
			case None =>
				new CompositeScalingImage(variants, Lazy.initialized(Image.empty), size,
					Pair.twice(NumericSpan.singleValue(0.0)))
				
			// Case: Only one image
			case Some(Left((maxSize, onlyImage))) =>
				new CompositeScalingImage(variants, onlyImage, size, maxSize.xyPair.map { NumericSpan(0.0, _) })
				
			// Case: Multiple images => Displays the best image for the targeted size
			case Some(Right(variants)) =>
				val displayedIndex = variants.findIndexWhere { _._1.forAllDimensionsWith(size) { _ >= _ } }
					.getOrElse(variants.size - 1)
				val (maxSize, image) = variants(displayedIndex)
				val smallerSize = if (displayedIndex > 0) variants(displayedIndex - 1)._1 else Size.zero
				
				new CompositeScalingImage(variants, image, size,
					smallerSize.xyPair.mergeWith(maxSize.xyPair) { NumericSpan(_, _) })
		}
}

/**
  * An image which displays an image of multiple options, based on the applied image scaling.
  * This allows, for example, one to display more details in a larger images while
  * making a small image more pronounced and easily distinguishable.
  * @author Mikko Hilpinen
  * @since 21.12.2024, v4.2
  */
class CompositeScalingImage(variants: Seq[(Size, Lazy[Image])], lazyAppliedImage: Lazy[Image], override val size: Size,
                            applicableSizeRanges: Pair[Span[Double]])
	extends Image with ImageLike[CompositeScalingImage]
{
	// ATTRIBUTES   --------------------
	
	/**
	  * The currently applied / displayed 'variants' index
	  */
	private lazy val appliedIndex = variants.indexWhere { _._1.forAllDimensionsWith(size) { _ >= _ } }
	
	private lazy val lowerSizeThreshold =
		if (appliedIndex > 0) Some(Size(applicableSizeRanges.map { _.start })) else None
		
	private lazy val maxSize = variants.lastOption match {
		case Some((size, _)) => size
		case None => Size.zero
	}
	
	override lazy val maxScaling = {
		if (size.isZero)
			None
		else
			Some(maxSize.xyPair.mergeWith(size.xyPair) { (max, len) => if (len == 0) None else Some(max / len) }.flatten
				.min.max(1.0))
	}
	
	
	// COMPUTED ------------------------
	
	private def wrapped = lazyAppliedImage.value
	
	
	// IMPLEMENTED  --------------------
	
	override def self = this
	override def empty = CompositeScalingImage.empty
	
	override def isEmpty = size.dimensions.exists { _ < 1.0 } || wrapped.isEmpty
	
	override def bounds = wrapped.bounds
	override def origin = wrapped.origin
	override def specifiesOrigin = wrapped.specifiesOrigin
	
	override def alpha = wrapped.alpha
	
	override def pixels = wrapped.pixels
	override def shade = wrapped.shade
	
	override def toImage = this
	override def toConcreteImage = wrapped.toConcreteImage
	override def toAwt = wrapped.toAwt
	
	override def downscaled =
		if (size.existsDimensionWith(maxSize) { _ > _ }) fullSized else this
	override def upscaled = if (size.existsDimensionWith(maxSize) { _ < _ }) fullSized else this
	override def fullSized = {
		// Case: Already at full size
		if (size == maxSize || variants.isEmpty)
			this
		else {
			val (newSize, newImage) = variants.last
			val lastSize = {
				if (variants.hasSize > 1)
					variants(variants.size - 2)._1
				else
					Size.zero
			}
			new CompositeScalingImage(variants, newImage, newSize,
				lastSize.xyPair.mergeWith(newSize.xyPair) { NumericSpan(_, _) })
		}
	}
	
	override def withMinimumResolution = {
		// Case: Currently displayed with full resolution => No downscaling may be applied
		if (size.forAllDimensionsWith(maxSize) { _ >= _ })
			this
		else {
			val remainingSmallerVariants = variants.takeWhile { _._1.forAllDimensionsWith(size) { _ < _ } }
			val modifiedSizeRange = applicableSizeRanges.mergeWith(size.xyPair) { _.withEnd(_) }
			new CompositeScalingImage(remainingSmallerVariants, lazyAppliedImage.map { _.withMinimumResolution }, size,
				modifiedSizeRange)
		}
	}
	
	override def withoutSpecifiedOrigin = mapPreservingSize { _.withoutSpecifiedOrigin }
	
	override def flippedHorizontally = mapPreservingSize { _.flippedHorizontally }
	override def flippedVertically = mapPreservingSize { _.flippedVertically }
	
	override def cropped = map { _.cropped }
	
	override def pixelAt(point: Point, relativeToOrigin: Boolean) = wrapped.pixelAt(point, relativeToOrigin)
	override def pixelsAt(area: Bounds, relativeToOrigin: Boolean) = wrapped.pixelsAt(area, relativeToOrigin)
	
	override def drawWith(drawer: Drawer, position: Point, transformation: Option[Matrix2D]) =
		wrapped.drawWith(drawer, position, transformation)
	override def drawSubImageWith(drawer: Drawer, subRegion: Bounds, position: Point) =
		wrapped.drawSubImageWith(drawer, subRegion, position)
	
	override def withOrigin(newOrigin: Point) = {
		val newRelativeOrigin = newOrigin / size
		mapPreservingSize { image => image.withOrigin(newRelativeOrigin * image.size) }
	}
	override def mapOrigin(f: (Point, Size) => Point) =
		mapPreservingSize { _.mapOrigin(f) }
	
	override def withSize(newSize: Size) = {
		// Case: No change
		if (newSize == size)
			this
		// Case: Image-swapping is not supported => Applies simple scaling
		else if (variants.hasSize <= 1)
			withSizeSameImage(newSize)
		else {
			// Checks whether the displayed image should be changed
			// Case: Scaled size remains within the current image's range
			if (applicableSizeRanges.forallWith(newSize.xyPair) { _ contains _ }) {
				// Case: The new scaling just touches the lower range
				//       => Displays the smaller image with full resolution instead
				if (lowerSizeThreshold.contains(newSize))
					withSizeDisplayingIndex(newSize, appliedIndex - 1)
				// Case: Continues to display the current image
				else
					withSizeSameImage(newSize)
			}
			// Case: Scaled size is outside the current image's supported size range
			else {
				// Finds the new image to display
				val newAppliedIndex = variants
					.findIndexWhere { case (size, _) => size.forAllDimensionsWith(newSize) { _ >= _ } }
					.getOrElse(variants.size - 1)
				
				// Case: Couldn't find another image => Continues displaying the current image
				if (newAppliedIndex == appliedIndex)
					withSizeSameImage(newSize)
				// Case: Found another image => Swaps to it
				else
					withSizeDisplayingIndex(newSize, newAppliedIndex)
			}
		}
	}
	override def *(scaling: HasDoubleDimensions) = withSize(size * scaling)
	override def *(mod: Double): CompositeScalingImage = this * Vector2D.twice(mod)
	override def /(divider: HasDoubleDimensions) =
		this * divider.dimensions.map { len => if (len == 0) 1.0 else 1/len }
	
	override def withMaxSourceResolution(maxResolution: Size) = {
		// Case: Has 1 or fewer variants => Delegates source resolution change to the wrapped image
		if (variants.hasSize <= 1)
			mapPreservingSize { _.withMaxSourceResolution(maxResolution) }
		// Case: No change is expected
		else if (maxResolution.forAllDimensionsWith(maxSize) { _ >= _ })
			this
		// Case: Resolution change affects this image => Modifies this image
		else {
			// Some variants may remain unaffected
			val preservedVariants = variants.takeWhile { _._1.forAllDimensionsWith(maxResolution) { _ <= _ } }
			// Others need to be modified
			val modifiedVariants = variants.drop(preservedVariants.size)
				.takeTo { _._1.forAllDimensionsWith(maxResolution) { _ >= _ } }
				.map { case (_, image) =>
					val newImage = image.value.withMaxSourceResolution(maxResolution)
					newImage.size -> Lazy.initialized(newImage)
				}
			CompositeScalingImage._apply(preservedVariants ++ modifiedVariants, size)
		}
	}
	
	override def croppedToFitWithin(maxArea: HasDoubleDimensions) =
		_croppedToFitWithin { _.fitsWithin(maxArea) } { size => Size.from(maxArea).fitsWithin(size) } {
			_.croppedToFitWithin(maxArea) } { _.croppedToFitWithin(maxArea) }
	override def croppedToFitWithin(maxLength: Dimension[Double]) =
		_croppedToFitWithin { _.fitsWithin(maxLength) } { size => size(maxLength.axis) >= maxLength.value } {
			_.croppedToFitWithin(maxLength) } { _.croppedToFitWithin(maxLength) }
	
	override def withAlpha(newAlpha: Double) = {
		val current = alpha
		if (current == 0)
			mapPreservingSize { _.withAlpha(newAlpha) }
		else {
			val alphaMod = newAlpha / current
			mapPreservingSize { _.timesAlpha(alphaMod) }
		}
	}
	override def mapAlpha(f: Double => Double) = mapPreservingSize { _.mapAlpha(f) }
	
	override def subImage(area: Bounds) = {
		area.overlapWith(Bounds(Point.origin, size)).filter { _.size.isPositive } match {
			case Some(overlap) =>
				val relativeArea = overlap / size
				map { img => img.subImage(relativeArea * img.size) }
				
			case None => empty
		}
	}
	override def withCanvasSize(canvasSize: Size, alignment: Alignment) =
		CompositeScalingImage.from(wrapped.withCanvasSize(canvasSize, alignment))
	
	override def mutatePixels(preserveShade: Boolean)
	                         (pixelsTransform: Mutate[Pixels])(originTransform: (Point, Size) => Point) =
		map { _.mutatePixels(preserveShade)(pixelsTransform)(originTransform) }
	override def mapEachPixel(f: Color => Color) = mapPreservingSize { _.mapEachPixel(f) }
	override def mapPixelsWithIndex(f: (Color, Pair[Int]) => Color) =
		mapPreservingSize { _.mapPixelsWithIndex(f) }
	override def mapPixelPoints(f: (Color, Point) => Color) =
		mapPreservingSize { _.mapPixelPoints(f) }
	override def mapArea(area: Area2D)(f: Color => Color) = mapPreservingSize { image =>
		if (image.size.isPositive) {
			val toAreaScaling = size / image.size
			if (toAreaScaling.isIdentity)
				image.mapArea(area)(f)
			else
				image.mapArea(p => area.contains(p * toAreaScaling))(f)
		}
		else
			image
	}
	
	override def paintedOver[U](paint: (Drawer, Size) => U) =
		mapPreservingSize { _.paintedOver(paint) }
	
	override def withOverlay(overlayImage: Image, overlayPosition: Point, relativeToOrigin: Boolean) = {
		val correctedPosition = if (relativeToOrigin) overlayPosition + origin else overlayPosition
		mapPreservingSize { image =>
			val scaling = image.size / size
			if (scaling.isIdentity)
				image.withOverlay(overlayImage, correctedPosition, relativeToOrigin = false)
			else
				image.withOverlay(overlayImage * scaling, correctedPosition * scaling, relativeToOrigin = false)
		}
	}
	override def withBackground(color: Color) = mapPreservingSize { _.withBackground(color) }
	
	override def filterWith(op: BufferedImageOp) = mapPreservingSize { _.filterWith(op) }
	
	
	// OTHER    ------------------------
	
	private def _croppedToFitWithin[A](testIfFits: Size => Boolean)(testIfContains: Size => Boolean)
	                                  (cropImage: Image => Image)(cropSize: Size => Size) =
	{
		// This operation might not affect all images
		val preservedVariants = variants.takeWhile { case (size, _) => testIfFits(size) }
		// Modifies those images that are affected by it.
		// May also remove some of the larger images.
		val modifiedVariants = variants.drop(preservedVariants.size)
			.takeTo { case (size, _) => testIfContains(size) }.zipWithIndex
			.map { case ((maxSize, lazyImage), modIndex) =>
				// Case: Modifying the currently active image => Applies cropping to the current scaled version
				if (modIndex + preservedVariants.size == appliedIndex) {
					val newImage = cropImage(wrapped)
					val scalingToMax = maxSize / size
					(newImage.size * scalingToMax, Lazy { newImage * scalingToMax }, Some(newImage))
				}
				// Case: Modifying another image => Applies cropping to the fully scaled version
				else {
					val image = lazyImage.value
					val newImage = cropImage(image)
					(newImage.size, Lazy.initialized(newImage), None)
				}
			}
		
		val newVariants = preservedVariants ++ modifiedVariants.view.map { case (size, img, _) => size -> img }
		modifiedVariants.findMap { _._3 } match {
			// Case: Modified the currently displayed image => Keeps it displayed
			case Some(newPrimaryImage) =>
				val sizeRange = {
					if (appliedIndex == 0)
						newVariants.head._1.xyPair.map { NumericSpan(0.0, _) }
					else
						newVariants(appliedIndex - 1)._1.xyPair
							.mergeWith(newVariants(appliedIndex)._1.xyPair) { NumericSpan(_, _) }
				}
				new CompositeScalingImage(newVariants, Lazy.initialized(newPrimaryImage), newPrimaryImage.size,
					sizeRange)
			
			case None => CompositeScalingImage._apply(newVariants, cropSize(size))
		}
	}
	
	/**
	  * Changes the size of this image, keeping the same displayed image.
	  * Assumes that image-swap shouldn't or can't be applied.
	  * @param newSize New size of this image
	  * @return Copy of this image with the specified size, keeping the same displayed image
	  */
	private def withSizeSameImage(newSize: Size) =
		new CompositeScalingImage(variants, lazyAppliedImage.map { _.withSize(newSize) }, newSize, applicableSizeRanges)
	/**
	  * Changes the size of this image, swapping to a specific variant
	  * @param newSize New size of this image
	  * @param variantIndex Index of the new variant to display
	  * @return Copy of this image with the specified size and displayed variant
	  */
	private def withSizeDisplayingIndex(newSize: Size, variantIndex: Int) = {
		val (newMaxSize, newImage) = variants(variantIndex)
		val smallerSize = if (variantIndex > 0) variants(variantIndex - 1)._1 else Size.zero
		
		new CompositeScalingImage(variants, newImage.map { _.withSize(newSize) }, newSize,
			smallerSize.xyPair.mergeWith(newMaxSize.xyPair) { NumericSpan(_, _) })
	}
	
	/**
	  * Applies a mapping function that changes the size of this image
	  * @param f A mapping function to apply
	  * @return A mapped copy of this image
	  */
	private def map(f: Mutate[Image]) = {
		if (variants.isEmpty) {
			val newImage = f(wrapped)
			val newSize = newImage.size
			new CompositeScalingImage(variants, Lazy.initialized(newImage), newSize,
				newSize.xyPair.map { NumericSpan(0.0, _) })
		}
		else {
			val newVariants = variants.map { case (_, img) =>
				val newImage = f(img.value)
				val maxSize = newImage.maxScaling match {
					case Some(maxScaling) => newImage.size * maxScaling
					case None => newImage.size
				}
				maxSize -> Lazy.initialized(newImage)
			}
			val (currentMaxSize, currentImage) = newVariants(appliedIndex)
			val smallerSize = if (appliedIndex > 0) newVariants(appliedIndex - 1)._1 else Size.zero
			
			// NB: Assumes that the variant ordering is preserved
			new CompositeScalingImage(newVariants, currentImage, currentImage.value.size,
				smallerSize.xyPair.mergeWith(currentMaxSize.xyPair) { NumericSpan(_, _) })
		}
	}
	/**
	  * Applies a mapping function that doesn't affect this image's size
	  * @param f A mapping function to apply
	  * @return A (lazily) mapped copy of this image
	  */
	private def mapPreservingSize(f: Mutate[Image]) =
		new CompositeScalingImage(variants.map { case (size, img) => size -> img.map(f) }, lazyAppliedImage.map(f),
			size, applicableSizeRanges)
}
