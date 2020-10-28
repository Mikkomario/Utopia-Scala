package utopia.reflection.component.reach.label

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.image.Image
import utopia.reflection.component.drawing.view.ImageViewDrawer
import utopia.reflection.component.reach.factory.ComponentFactoryFactory
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.MutableCustomDrawReachComponent
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets

object MutableImageLabel extends ComponentFactoryFactory[MutableImageLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new MutableImageLabelFactory(hierarchy)
}

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

/**
  * A mutable implementation of a label that draws an image
  * @author Mikko Hilpinen
  * @since 28.10.2020, v2
  */
class MutableImageLabel(override val parentHierarchy: ComponentHierarchy, initialImage: Image,
						initialInsets: StackInsets = StackInsets.zero, initialAlignment: Alignment = Alignment.Center,
						override val allowUpscaling: Boolean = true, override val useLowPrioritySize: Boolean = false)
	extends MutableCustomDrawReachComponent with ImageLabelLike
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * Pointer to this label's displayed image
	  */
	val imagePointer = new PointerWithEvents(initialImage)
	/**
	  * Pointer to the insets placed around the image in this label
	  */
	val insetsPointer = new PointerWithEvents(initialInsets)
	/**
	  * Pointer to the alignment used when positioning the image in this label
	  */
	val alignmentPointer = new PointerWithEvents(initialAlignment)
	
	
	// INITIAL CODE	--------------------------
	
	// Adds image drawing
	addCustomDrawer(ImageViewDrawer(imagePointer, insetsPointer, alignmentPointer, useUpscaling = allowUpscaling))
	
	// Updates and repaints this label when values change
	imagePointer.addListener { change =>
		if (change.compareBy { _.size } && change.compareBy { _.sourceResolution })
			repaint()
		else
			revalidateAndThen { repaint() }
	}
	insetsPointer.addListener { _ => revalidateAndThen { repaint() } }
	alignmentPointer.addListener { _ => repaint() }
	
	
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
