package utopia.firmament.drawing.immutable

import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ViewImageDrawer.ViewImageDrawerFactory
import utopia.firmament.factory.FramedFactory
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.view.immutable.View
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.{DrawLevel, Drawer, FromDrawLevelFactory}
import utopia.genesis.image.Image
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.transform.LinearTransformable

import scala.annotation.unused
import scala.language.implicitConversions

object ImageDrawer2
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * A factory used for creating new immutable image drawers.
	  * Set up with default settings.
	  */
	val factory = ImageDrawerFactory()
	
	
	// IMPLICIT -------------------------------
	
	implicit def objectToFactory(@unused o: ImageDrawer2.type): ImageDrawerFactory = factory
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param image The image to draw
	  * @return A drawer that draws the specified image with default settings
	  */
	// Once the deprecated apply version is removed, this function may also be removed
	def apply(image: Image) = factory(image)
	
	@deprecated("Please use the factory approach instead", "v1.3")
	def apply(image: Image, insets: StackInsets = StackInsets.any, alignment: Alignment = Alignment.Center,
	          drawLevel: DrawLevel = Normal, useUpscaling: Boolean = true) =
		ImageDrawerFactory(None, insets, alignment, drawLevel, useUpscaling)(image)
	
	
	// NESTED   -------------------------------
	
	case class ImageDrawerFactory(transformation: Option[Matrix2D] = None, insets: StackInsets = StackInsets.any,
	                              alignment: Alignment = Alignment.Center, drawLevel: DrawLevel = DrawLevel.default,
	                              upscales: Boolean = false)
		extends FromAlignmentFactory[ImageDrawerFactory] with FramedFactory[ImageDrawerFactory]
			with FromDrawLevelFactory[ImageDrawerFactory] with LinearTransformable[ImageDrawerFactory]
	{
		// COMPUTED ---------------------------
		
		/**
		  * @return Copy of this factory that allows images to be scaled up,
		  *         if that is required to fill the targeted draw area
		  */
		def upscaling = copy(upscales = true)
		
		/**
		  * @return Copy of this factory that supports view-based drawing
		  */
		def view =
			ViewImageDrawerFactory(View.fixed(transformation), View.fixed(insets), View.fixed(alignment), drawLevel,
				upscales)
		
		
		// IMPLEMENTED  -----------------------
		
		override def identity: ImageDrawerFactory = this
		
		override def withInsets(insets: StackInsetsConvertible): ImageDrawerFactory = copy(insets = insets.toInsets)
		override def apply(alignment: Alignment): ImageDrawerFactory = copy(alignment = alignment)
		override def apply(drawLevel: DrawLevel): ImageDrawerFactory = copy(drawLevel = drawLevel)
		override def transformedWith(transformation: Matrix2D): ImageDrawerFactory = {
			val newTransform = this.transformation match {
				case Some(t) => t * transformation
				case None => transformation
			}
			copy(transformation = Some(newTransform))
		}
		
		
		// OTHER    ---------------------------
		
		/**
		  * @param image The image to draw
		  * @return A drawer that draws that image, respecting the settings within this factory
		  */
		def apply(image: Image): ImageDrawer2 =
			_ImageDrawer(image, transformation, insets, alignment, drawLevel, upscales)
	}
	
	private case class _ImageDrawer(image: Image, transformation: Option[Matrix2D], insets: StackInsets,
	                                alignment: Alignment, drawLevel: DrawLevel, useUpscaling: Boolean)
		extends ImageDrawer2
}

/**
  * A common trait for image drawer implementations
  * @author Mikko Hilpinen
  * @since 25.3.2020, Reflection v1
  */
trait ImageDrawer2 extends CustomDrawer
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return The image being drawn by this drawer
	  */
	def image: Image
	/**
	  * @return A transformation applied to the image when drawn
	  */
	def transformation: Option[Matrix2D]
	/**
	  * @return The insets being placed around the image
	  */
	def insets: StackInsets
	/**
	  * @return Alignment that determines the position of the image
	  */
	def alignment: Alignment
	/**
	  * @return Whether the image is allowed to scale up from its normal size when presented with more space
	  *         (the natural resolution of the image is still respected).
	  */
	def useUpscaling: Boolean
	
	
	// IMPLEMENTED	------------------------
	
	override def opaque = false
	
	override def draw(drawer: Drawer, bounds: Bounds) = {
		// Calculates the sizes for the transformed image state
		val rawBounds = image.bounds
		val imageBounds = transformation match {
			case Some(t) => (rawBounds * t).bounds
			case None => rawBounds
		}
		val defaultSize = imageBounds.size + insets.optimal.total
		
		// Calculates the scaling to apply
		val scaling = {
			// Case: May downscale, won't up-scale
			if (!useUpscaling || defaultSize.existsDimensionWith(bounds.size) { _ >= _ }) {
				val maxSize = bounds.size - insets.min.total
				(maxSize / imageBounds.size).minDimension min 1.0
			}
			// Case: Up-scales
			else {
				// Limits up-scaling to maximum insets
				val targetSize = bounds.size - insets.mapToInsets { l => l.max.getOrElse(l.optimal) }.total
				// Limits the scaling to the original image resolution (unless already scaled above that)
				val maxScaling = (Vector2D.identity / image.scaling).minDimension
				// Will never downscale (otherwise could because of the increased insets)
				((targetSize / imageBounds.size).minDimension min maxScaling) max 1.0
			}
		}
		
		// Draws the image (unless applied scaling is 0 or less)
		if (scaling > 0) {
			// Attempts to round the image bounds before drawing in order to reduce artifacts
			val scaledImageBounds = (imageBounds * scaling).round
			
			// Calculates the position of the transformed shape's top-left corner (includes scaling)
			val topLeftPosition = alignment.positionWithInsets(scaledImageBounds.size, bounds, insets).position
			// The image draw-position is set so that the image origin is preserved, however
			val position = (topLeftPosition + scaledImageBounds.position).round
			
			// Calculates the final scaling & transformation to apply
			val appliedScaling = Some((scaledImageBounds.size / imageBounds.size).toVector)
				.filterNot { _ ~== Vector2D.identity }
				.map { s => Matrix2D.scaling(s) }
			val appliedTransformation = transformation match {
				case Some(t) =>
					Some(appliedScaling match {
						case Some(s) => t * s
						case None => t
					})
				case None => appliedScaling
			}
			
			// Draws the image
			image.drawWith(drawer, position, appliedTransformation)
		}
	}
}
