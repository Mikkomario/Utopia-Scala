package utopia.genesis.image

import utopia.flow.collection.immutable.Pair
import utopia.flow.util.Mutate
import utopia.genesis.graphics.Drawer
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape1d.Dimension
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import java.awt.image.BufferedImageOp

/**
  * Common trait for image implementations that are based on wrapping another image
  * @tparam Repr Concrete implementing type
  * @author Mikko Hilpinen
  * @since 21.12.2024, v4.2
  */
trait ImageWrapper[+Repr] extends ImageLike[Repr]
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return The wrapped image implementation
	  */
	protected def wrapped: Image
	
	/**
	  * @param image New image implementation to wrap
	  * @return Copy of this item with the specified image implementation
	  */
	protected def withImage(image: Image): Repr
	
	
	// IMPLEMENTED  ------------------------
	
	override def isEmpty = wrapped.isEmpty
	
	override def size = wrapped.size
	override def bounds = wrapped.bounds
	override def maxScaling = wrapped.maxScaling
	
	override def origin = wrapped.origin
	override def specifiesOrigin = wrapped.specifiesOrigin
	
	override def alpha = wrapped.alpha
	
	override def pixels = wrapped.pixels
	override def shade = wrapped.shade
	
	override def toConcreteImage = wrapped.toConcreteImage
	override def toAwt = wrapped.toAwt
	
	override def pixelAt(point: Point, relativeToOrigin: Boolean) = wrapped.pixelAt(point, relativeToOrigin)
	override def pixelsAt(area: Bounds, relativeToOrigin: Boolean) = wrapped.pixelsAt(area, relativeToOrigin)
	
	override def drawWith(drawer: Drawer, position: Point, transformation: Option[Matrix2D]) =
		wrapped.drawWith(drawer, position, transformation)
	override def drawSubImageWith(drawer: Drawer, subRegion: Bounds, position: Point) =
		wrapped.drawSubImageWith(drawer, subRegion, position)
	
	override def downscaled = mapImage { _.downscaled }
	override def upscaled = mapImage { _.upscaled }
	override def fullSized = mapImage { _.fullSized }
	override def withMinimumResolution = mapImage { _.withMinimumResolution }
	
	override def withoutSpecifiedOrigin = mapImage { _.withoutSpecifiedOrigin }
	
	override def cropped = mapImage { _.cropped }
	
	override def flippedHorizontally = mapImage { _.flippedHorizontally }
	override def flippedVertically = mapImage { _.flippedVertically }
	
	override def withOrigin(newOrigin: Point) = mapImage { _.withOrigin(newOrigin) }
	override def mapOrigin(f: (Point, Size) => Point) = mapImage { _.mapOrigin(f) }
	
	override def withSize(size: Size) = mapImage { _.withSize(size) }
	override def *(mod: Double) = mapImage { _ * mod }
	override def *(scaling: HasDoubleDimensions) = mapImage { _ * scaling }
	override def /(divider: HasDoubleDimensions) = mapImage { _ * divider }
	
	override def withMaxSourceResolution(maxResolution: Size) =
		mapImage { _.withMaxSourceResolution(maxResolution) }
	
	override def croppedToFitWithin(maxArea: HasDoubleDimensions) = mapImage { _.croppedToFitWithin(maxArea) }
	override def croppedToFitWithin(maxLength: Dimension[Double]) = mapImage { _.croppedToFitWithin(maxLength) }
	
	override def withAlpha(newAlpha: Double) = mapImage { _.withAlpha(newAlpha) }
	override def mapAlpha(f: Double => Double) = mapImage { _.mapAlpha(f) }
	
	override def subImage(area: Bounds) = mapImage { _.subImage(area) }
	override def withCanvasSize(canvasSize: Size, alignment: Alignment) =
		mapImage { _.withCanvasSize(canvasSize, alignment) }
	
	override def mutatePixels(preserveShade: Boolean)
	                         (pixelsTransform: Mutate[Pixels])(originTransform: (Point, Size) => Point) =
		mapImage { _.mutatePixels(preserveShade)(pixelsTransform)(originTransform) }
	override def mapEachPixel(f: Color => Color) = mapImage { _.mapEachPixel(f) }
	override def mapPixelsWithIndex(f: (Color, Pair[Int]) => Color) = mapImage { _.mapPixelsWithIndex(f) }
	override def mapPixelPoints(f: (Color, Point) => Color) = mapImage { _.mapPixelPoints(f) }
	override def mapArea(area: Area2D)(f: Color => Color) = mapImage { _.mapArea(area)(f) }
	
	override def paintedOver[U](paint: (Drawer, Size) => U) = mapImage { _.paintedOver(paint) }
	override def withOverlay(overlayImage: Image, overlayPosition: Point, relativeToOrigin: Boolean) =
		mapImage { _.withOverlay(overlayImage, overlayPosition, relativeToOrigin) }
	override def withBackground(color: Color) = mapImage { _.withBackground(color) }
	
	override def filterWith(op: BufferedImageOp) = mapImage { _.filterWith(op) }
	
	
	// OTHER    ------------------------
	
	/**
	  * Modifies this item by modifying the wrapped image
	  * @param f A mapping function applied to the currently wrapped image
	  * @return Copy of this item with a modified image
	  */
	protected def mapImage(f: Mutate[Image]) = withImage(f(wrapped))
}
