package utopia.reach.component.label

import utopia.genesis.image.Image
import utopia.reflection.component.drawing.template.CustomDrawable2
import utopia.reach.component.template.ReachComponentLike
import utopia.reflection.component.template.layout.stack.CachingStackable2
import utopia.reflection.shape.stack.{StackInsets, StackSize}

/**
  * A common trait for image label implementations
  * @author Mikko Hilpinen
  * @since 27.10.2020, v0.1
  */
trait ImageLabelLike extends ReachComponentLike with CustomDrawable2 with CachingStackable2
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
		val raw =
		{
			if (allowUpscaling)
				StackSize.downscaling(image.size.ceil, image.sourceResolution)
			else
				StackSize.downscaling(image.size.ceil)
		}
		if (useLowPrioritySize) raw.shrinking else raw
	}
}
