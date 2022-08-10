package utopia.reach.component.label.image

import utopia.genesis.image.Image
import utopia.reach.component.factory.ComponentFactoryFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.reflection.component.drawing.immutable.ImageDrawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.StackInsets

object ImageLabel extends ComponentFactoryFactory[ImageLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ImageLabelFactory(hierarchy)
}

class ImageLabelFactory(parentHierarchy: ComponentHierarchy)
{
	/**
	  * Creates a new image label
	  * @param image Drawn image
	  * @param insets Insets placed around the image (default = always 0)
	  * @param alignment Alignment used when placing the image (default = Center)
	  * @param additionalCustomDrawers Additional custom drawers assigned to this label
	  * @param allowUpscaling Whether the image should be allowed to scale up to it's source resolution (default = true)
	  * @param useLowPrioritySize Whether low priority size constraints should be used for this image
	  * @return A new label
	  */
	def apply(image: Image, insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
			  additionalCustomDrawers: Vector[CustomDrawer] = Vector(), allowUpscaling: Boolean = true,
			  useLowPrioritySize: Boolean = false) =
		new ImageLabel(parentHierarchy, image, insets, alignment, additionalCustomDrawers, allowUpscaling,
			useLowPrioritySize)
}

/**
  * A label that draws an image
  * @author Mikko Hilpinen
  * @since 27.10.2020, v0.1
  */
class ImageLabel(override val parentHierarchy: ComponentHierarchy, override val image: Image,
				 override val insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
				 additionalCustomDrawers: Vector[CustomDrawer] = Vector(), override val allowUpscaling: Boolean = true,
				 override val useLowPrioritySize: Boolean = false)
	extends CustomDrawReachComponent with ImageLabelLike
{
	// ATTRIBUTES	------------------------------
	
	override val customDrawers = ImageDrawer(image, insets, alignment, useUpscaling = allowUpscaling) +:
		additionalCustomDrawers
	
	
	// IMPLEMENTED	------------------------------
	
	override def updateLayout() = ()
}
