package utopia.reflection.component.swing.label

import utopia.firmament.component.display.RefreshableWithPointer
import utopia.firmament.context.BaseContext
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.graphics.Drawer
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackLeaf}
import utopia.reflection.shape.stack.StackSize

object ImageLabel
{
	/**
	  * Creates a new label using contextual settings
	  * @param image Image presented in this label
	  * @param alwaysFillsArea Whether image should fill the whole area (default = true)
	  * @param isLowPriority Whether the image should be shrank easily when adjusting component sizes (default = false)
	  * @param context Component creation context
	  * @return A new label
	  */
	def contextual(image: Image, alwaysFillsArea: Boolean = true, isLowPriority: Boolean = false)
				  (implicit context: BaseContext) =
		new ImageLabel(image, alwaysFillsArea, context.allowImageUpscaling, isLowPriority)
}

/**
  * This label shows an image
  * @author Mikko Hilpinen
  * @since 7.7.2019, v1+
  * @param initialImage The initially displayed image
  * @param alwaysFillArea Whether the whole label area should be filled with the image (default = true)
  * @param allowUpscaling Whether the image should be allowed to scale above its size (default = false)
  * @constructor Creates a new image label with specified settings (always fill area and upscaling allowing)
  */
class ImageLabel(initialImage: Image, alwaysFillArea: Boolean = true, allowUpscaling: Boolean = false,
				 isLowPriority: Boolean = false)
	extends Label with CachingReflectionStackable with RefreshableWithPointer[Image] with ReflectionStackLeaf
{
	// ATTRIBUTES	-----------------
	
	override val contentPointer = new PointerWithEvents(initialImage)
	
	private var scaledImage = initialImage
	private var relativeImagePosition = Point.origin
	
	
	// INITIAL CODE	-----------------
	
	addCustomDrawer(new ImageDrawer)
	addResizeListener(updateLayout())
	
	// Revalidates this component whenever image changes (although only if image size changes as well)
	contentPointer.addContinuousListener { event =>
		if (event.newValue.size != event.oldValue.size)
			revalidate()
		else
			updateLayout()
	}
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return The currently displayed image in this label
	  */
	def image = content
	/**
	  * Updates displayed image
	  * @param newImage The new image to be displayed in this label
	  */
	def image_=(newImage: Image) = content = newImage
	
	
	// IMPLEMENTED	-----------------
	
	override protected def updateVisibility(visible: Boolean) = super[Label].visible_=(visible)
	
	override def updateLayout() =
	{
		// Updates image scaling to match this label's size
		if (image.size.ceil == size)
			scaledImage = image
		else if (alwaysFillArea || !image.size.fitsWithin(size))
			scaledImage = image.withSize(size)
		else
			scaledImage = image
		
		relativeImagePosition = (size - scaledImage.size).toPoint / 2
		repaint()
	}
	
	override def calculatedStackSize =
	{
		// Optimal size is always set to image size
		// Upscaling may also be allowed (limited if upscaling is not allowed and image must fill area)
		val imageSize = image.size.ceil
		val isLimited = alwaysFillArea && !allowUpscaling
		
		val raw = if (isLimited) StackSize.downscaling(imageSize) else StackSize.any(imageSize)
		if (isLowPriority) raw.shrinking else raw
	}
	
	
	// NESTED CLASSES	------------
	
	private class ImageDrawer extends CustomDrawer
	{
		override def opaque = false
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) = {
			// Draws the image with prepared settings
			scaledImage.drawWith(drawer, bounds.position + relativeImagePosition)
		}
	}
}
