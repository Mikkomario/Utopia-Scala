package utopia.reflection.component.swing.label

import utopia.firmament.component.display.RefreshableWithPointer
import utopia.firmament.component.image.ImageComponent
import utopia.firmament.context.BaseContext
import utopia.firmament.drawing.view.ViewImageDrawer
import utopia.firmament.model.stack.StackInsets
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackLeaf}

object ImageLabel2
{
	/**
	  * Creates a new label using contextual settings
	  * @param image Image presented in this label
	  * @param isLowPriority Whether the image should be shrank easily when adjusting component sizes (default = false)
	  * @param context Component creation context
	  * @return A new label
	  */
	def contextual(image: Image, isLowPriority: Boolean = false)(implicit context: BaseContext) =
		new ImageLabel2(image, context.allowImageUpscaling, isLowPriority)
}

/**
  * This label shows an image
  * @author Mikko Hilpinen
  * @since 7.7.2019, v1+
  * @param initialImage The initially displayed image
  * @param allowUpscaling Whether the image should be allowed to scale above its size (default = false)
  * @constructor Creates a new image label with specified settings (always fill area and upscaling allowing)
  */
class ImageLabel2(initialImage: Image, override val allowUpscaling: Boolean = false,
                  override val useLowPrioritySize: Boolean = false)
	extends Label with CachingReflectionStackable with ImageComponent with RefreshableWithPointer[Image]
		with ReflectionStackLeaf
{
	// ATTRIBUTES	-----------------
	
	override val contentPointer = new EventfulPointer(initialImage)
	/**
	  * Pointer that contains the currently displayed transformation applied to the drawn image
	  */
	val transformationPointer = EventfulPointer.empty[Matrix2D]()
	
	private val imageBoundsPointer = contentPointer.map { _.bounds }
	private val visualImageSizePointer = imageBoundsPointer.mergeWith(transformationPointer) { (b, t) =>
		t match {
			case Some(t) => (b * t).size
			case None => b.size
		}
	}
	
	
	// INITIAL CODE	-----------------
	
	addCustomDrawer(
		ViewImageDrawer.withTransformationView(transformationPointer).copy(upscales = allowUpscaling)(contentPointer))
	// TODO: Check whether repaint is required here
	// addResizeListener(repaint())
	
	// Revalidates this component whenever image changes (although only if image size changes as well)
	contentPointer.addContinuousListener { event =>
		if (event.newValue.size != event.oldValue.size)
			revalidate()
		else
			repaint()
	}
	transformationPointer.addContinuousAnyChangeListener {
		revalidate()
		repaint()
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
	
	override def visualImageSize: Size = visualImageSizePointer.value
	override def imageScaling: Vector2D = image.scaling
	override def insets: StackInsets = StackInsets.any
	
	override protected def updateVisibility(visible: Boolean) = super[Label].visible_=(visible)
	
	override def updateLayout() = ()
}
