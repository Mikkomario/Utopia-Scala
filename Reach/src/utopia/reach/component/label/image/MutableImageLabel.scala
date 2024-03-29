package utopia.reach.component.label.image

import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.image.Image
import utopia.reach.component.factory.ComponentFactoryFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.MutableCustomDrawReachComponent
import utopia.firmament.drawing.view.ImageViewDrawer
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.StackInsets

// TODO: Use ImageLabelSettings here
class MutableImageLabelFactory(parentHierarchy: ComponentHierarchy)
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
		new MutableImageLabel(parentHierarchy, image, insets, alignment, allowUpscaling, useLowPrioritySize)
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
class MutableImageLabel(override val parentHierarchy: ComponentHierarchy, initialImage: Image,
						initialInsets: StackInsets = StackInsets.zero, initialAlignment: Alignment = Alignment.Center,
						override val allowUpscaling: Boolean = true, override val useLowPrioritySize: Boolean = false)
	extends MutableCustomDrawReachComponent with ImageLabel
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * Pointer to this label's displayed image
	  */
	val imagePointer = new EventfulPointer(initialImage)
	/**
	  * Pointer to the insets placed around the image in this label
	  */
	val insetsPointer = new EventfulPointer(initialInsets)
	/**
	  * Pointer to the alignment used when positioning the image in this label
	  */
	val alignmentPointer = new EventfulPointer(initialAlignment)
	
	
	// INITIAL CODE	--------------------------
	
	// Adds image drawing
	addCustomDrawer(ImageViewDrawer(imagePointer, insetsPointer, alignmentPointer, useUpscaling = allowUpscaling))
	
	// Updates and repaints this label when values change
	imagePointer.addContinuousListener { change =>
		if (change.equalsBy { _.size } && change.equalsBy { _.sourceResolution })
			repaint()
		else
			revalidate()
	}
	insetsPointer.addContinuousListener { _ => revalidate() }
	alignmentPointer.addContinuousListener { _ => repaint() }
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return Alignment used when positioning the image in this label
	  */
	def alignment = alignmentPointer.value
	def alignment_=(newAlignment: Alignment) = alignmentPointer.value = newAlignment
	
	
	// IMPLEMENTED	--------------------------
	
	override def image = imagePointer.value
	def image_=(newImage: Image) = imagePointer.value = newImage
	
	override def insets = insetsPointer.value
	def insets_=(newInsets: StackInsets) = insetsPointer.value = newInsets
	
	override def updateLayout() = ()
}
