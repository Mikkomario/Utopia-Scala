package utopia.reach.component.label.image

import utopia.firmament.component.stack.CachingStackable
import utopia.genesis.image.Image
import utopia.reach.component.template.ReachComponentLike
import utopia.reflection.component.drawing.template.CustomDrawable
import utopia.reflection.shape.stack.{StackInsets, StackSize}

/**
  * A common trait for image label implementations
  * @author Mikko Hilpinen
  * @since 27.10.2020, v0.1
  */
trait ImageLabelLike extends ReachComponentLike with CustomDrawable with CachingStackable
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return The image drawn on this label
	  */
	def image: Image
	
	/**
	  * @return Insets placed around the image
	  */
	def insets: StackInsets
	
	/**
	  * @return Whether image should be allowed to scale beyond it's original size
	  *         (while still respecting source resolution)
	  */
	def allowUpscaling: Boolean
	
	/**
	  * @return Whether this label should use lower priority size constraints
	  */
	def useLowPrioritySize: Boolean
	
	
	// IMPLEMENTED	------------------------
	
	override def calculatedStackSize =
	{
		val raw = {
			if (allowUpscaling)
				StackSize.downscaling(image.size.ceil, image.sourceResolution)
			else
				StackSize.downscaling(image.size.ceil)
		}
		if (useLowPrioritySize) raw.shrinking else raw
	}
}
