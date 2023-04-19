package utopia.reach.component.label.image

import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.image.Image
import utopia.reach.component.factory.ComponentFactoryFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ImageViewDrawer
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.StackInsets

object ViewImageLabel extends ComponentFactoryFactory[ViewImageLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewImageLabelFactory(hierarchy)
}

class ViewImageLabelFactory(parentHierarchy: ComponentHierarchy)
{
	/**
	  * Creates a new image label
	  * @param imagePointer Pointer to the drawn image
	  * @param insetsPointer Pointer to the insets placed around the image (default = always 0)
	  * @param alignmentPointer Pointer to the alignment used when positioning the image in this label
	  *                         (default = always center)
	  * @param customDrawers Additional custom drawers assigned to this label (default = empty)
	  * @param allowUpscaling Whether the image should be allowed to scale up to its source resolution (default = true)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @return A new image label
	  */
	def apply(imagePointer: Changing[Image], insetsPointer: Changing[StackInsets] = Fixed(StackInsets.zero),
	          alignmentPointer: Changing[Alignment] = Fixed(Alignment.Center),
	          customDrawers: Vector[CustomDrawer] = Vector(), allowUpscaling: Boolean = true,
	          useLowPrioritySize: Boolean = false) =
		new ViewImageLabel(parentHierarchy, imagePointer, insetsPointer, alignmentPointer, customDrawers,
			allowUpscaling, useLowPrioritySize)
	
	/**
	  * Creates a new image label
	  * @param imagePointer Pointer to the drawn image
	  * @param insets Insets placed around the image (default = always 0)
	  * @param alignment Alignment used when positioning the image in this label (default = center)
	  * @param customDrawers Additional custom drawers assigned to this label (default = empty)
	  * @param allowUpscaling Whether the image should be allowed to scale up to its source resolution (default = true)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @return A new image label
	  */
	def withStaticLayout(imagePointer: Changing[Image], insets: StackInsets = StackInsets.zero,
	                     alignment: Alignment = Alignment.Center,
	                     customDrawers: Vector[CustomDrawer] = Vector(), allowUpscaling: Boolean = true,
	                     useLowPrioritySize: Boolean = false) =
		apply(imagePointer, Fixed(insets), Fixed(alignment), customDrawers, allowUpscaling,
			useLowPrioritySize)
}

/**
  * A pointer-based label that draws an image
  * @author Mikko Hilpinen
  * @since 28.10.2020, v0.1
  */
class ViewImageLabel(override val parentHierarchy: ComponentHierarchy, imagePointer: Changing[Image],
                     insetsPointer: Changing[StackInsets], alignmentPointer: Changing[Alignment],
                     additionalCustomDrawers: Vector[CustomDrawer] = Vector(),
                     override val allowUpscaling: Boolean = true, override val useLowPrioritySize: Boolean = false)
	extends CustomDrawReachComponent with ImageLabelLike
{
	// ATTRIBUTES	---------------------------------
	
	val customDrawers = ImageViewDrawer(imagePointer, insetsPointer, alignmentPointer, useUpscaling = allowUpscaling) +:
		additionalCustomDrawers
	
	
	// INITIAL CODE	---------------------------------
	
	// Reacts to changes in the pointers
	imagePointer.addContinuousListener { change =>
		if (change.equalsBy { _.size } && change.equalsBy { _.sourceResolution })
			repaint()
		else
			revalidate()
	}
	insetsPointer.addContinuousListener { _ => revalidate() }
	alignmentPointer.addContinuousListener { _ => repaint() }
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return Current alignment used when positioning the image in this label
	  */
	def alignment = alignmentPointer.value
	
	
	// IMPLEMENTED	---------------------------------
	
	override def image = imagePointer.value
	
	override def insets = insetsPointer.value
	
	override def updateLayout() = ()
}
