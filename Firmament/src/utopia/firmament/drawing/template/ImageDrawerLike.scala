package utopia.firmament.drawing.template

import utopia.genesis.graphics.Drawer
import utopia.genesis.image.Image
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * A common trait for image drawer implementations
  * @author Mikko Hilpinen
  * @since 25.3.2020, Reflection v1
  */
@deprecated("Deprecated for removal. Replaced with ImageDrawer", "v1.3")
trait ImageDrawerLike extends CustomDrawer
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return The image being drawn by this drawer
	  */
	def image: Image
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
		// Calculates the size of the drawn image
		val defaultSize = image.size + insets.optimal.total
		val imageToDraw = {
			// Default case: No upscaling used or required
			if (!useUpscaling || defaultSize.existsDimensionWith(bounds.size) { _ >= _ }) {
				// Downscales if necessary
				Some(bounds.size - insets.min.total)
					.filter { _.sign.isPositive }
					.map { image.fittingWithin(_).roundSize }
			}
			else {
				// Case: Upscaling is required (still limited by original image resolution,
				// unless original image is already over source resolution)
				val imageSize = (bounds.size - insets.mapToInsets { l => l.max.getOrElse(l.optimal) }.total) topLeft
					(image.size bottomRight image.sourceResolution)
				Some(image.fittingWithin(imageSize, maximize = true).roundSize)
			}
		}
		
		// Draws the image
		imageToDraw.foreach { img =>
			val position = alignment.positionWithInsets(img.size, bounds, insets, fitWithinBounds = false).position
			// Since 'position' represents the desired top left corner of the drawn image,
			// has to adjust according to image origin
			img.drawWith(drawer, (position + img.origin).round)
		}
	}
}
