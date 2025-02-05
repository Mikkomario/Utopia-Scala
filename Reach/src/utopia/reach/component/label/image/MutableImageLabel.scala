package utopia.reach.component.label.image

import utopia.firmament.drawing.view.ViewImageDrawer
import utopia.firmament.model.stack.StackInsets
import utopia.flow.event.listener.ChangeListener
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.image.Image
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.factory.ComponentFactoryFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{MutableConcreteCustomDrawReachComponent, PartOfComponentHierarchy}

// TODO: Use ImageLabelSettings here
case class MutableImageLabelFactory(hierarchy: ComponentHierarchy) extends PartOfComponentHierarchy
{
	/**
	  * Creates a new image label
	  * @param image Image to draw
	  * @param insets Insets to place around the image (default = always 0)
	  * @param alignment Alignment to use when positioning the image (default = center)
	  * @param allowUpscaling Whether the image should be allowed to scale up (up to its original resolution)
	  *                       (default = true)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @return A new image label
	  */
	def apply(image: Image, insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
			  allowUpscaling: Boolean = true, useLowPrioritySize: Boolean = false) =
		new MutableImageLabel(hierarchy, image, insets, alignment, allowUpscaling, useLowPrioritySize)
}

object MutableImageLabel extends ComponentFactoryFactory[MutableImageLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new MutableImageLabelFactory(hierarchy)
}

/**
  * A mutable implementation of a label that draws an image
  * @author Mikko Hilpinen
  * @since 28.10.2020, v0.1
  */
class MutableImageLabel(override val hierarchy: ComponentHierarchy, initialImage: Image,
                        initialInsets: StackInsets = StackInsets.zero, initialAlignment: Alignment = Alignment.Center,
                        override val allowUpscaling: Boolean = true, override val useLowPrioritySize: Boolean = false)
	extends MutableConcreteCustomDrawReachComponent with ImageLabel
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * Pointer to this label's displayed image
	  */
	val imagePointer = EventfulPointer(initialImage)
	/**
	  * Pointer to the insets placed around the image in this label
	  */
	val insetsPointer = EventfulPointer(initialInsets)
	/**
	  * Pointer to the alignment used when positioning the image in this label
	  */
	val alignmentPointer = EventfulPointer(initialAlignment)
	/**
	  * Pointer to the transformation applied when drawing the image
	  */
	val transformationPointer = EventfulPointer.empty[Matrix2D]
	
	private val visualImageSizePointer = imagePointer.mergeWith(transformationPointer) { (img, t) =>
		t match {
			case Some(t) => (img.bounds * t).size
			case None => img.size
		}
	}
	
	private val revalidateListener = ChangeListener.onAnyChange { revalidate() }
	
	
	// INITIAL CODE	--------------------------
	
	// Adds image drawing
	addCustomDrawer(ViewImageDrawer
		.copy(insetsPointer = insetsPointer, alignmentView = alignmentPointer, upscales = allowUpscaling)
		.apply(imagePointer))
	
	// Updates and repaints this label when values change
	imagePointer.addContinuousListener { change =>
		if (change.equalsBy { _.size } && change.equalsBy { _.maxScaling })
			repaint()
		else
			revalidate()
	}
	transformationPointer.addListener(revalidateListener)
	insetsPointer.addListener(revalidateListener)
	alignmentPointer.addContinuousListener { _ => repaint() }
	
	
	// COMPUTED	------------------------------
	
	def image = imagePointer.value
	def image_=(newImage: Image) = imagePointer.value = newImage
	
	/**
	  * @return Alignment used when positioning the image in this label
	  */
	def alignment = alignmentPointer.value
	def alignment_=(newAlignment: Alignment) = alignmentPointer.value = newAlignment
	
	
	// IMPLEMENTED	--------------------------
	
	override def visualImageSize: Size = visualImageSizePointer.value
	override def maxScaling = image.maxScaling
	
	override def insets = insetsPointer.value
	def insets_=(newInsets: StackInsets) = insetsPointer.value = newInsets
	
	override def updateLayout() = ()
}
