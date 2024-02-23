package utopia.firmament.component.image

import utopia.firmament.component.Component
import utopia.firmament.component.stack.CachingStackable
import utopia.firmament.drawing.template.CustomDrawable
import utopia.firmament.model.stack.{StackInsets, StackSize}
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * A common trait for image label implementations
  * @author Mikko Hilpinen
  * @since 27.10.2020, Reach v0.1, added to Firmament 23.2.2024 at v1.3
  */
trait ImageComponent extends Component with CustomDrawable with CachingStackable
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return (Optimal / default) size of the image when drawn on the screen.
	  *         Determines (for example) how much space this component needs, optimally.
	  */
	def visualImageSize: Size
	/**
	  * @return Currently applied image scaling (on image level).
	  *         No additional transformation has been applied to this value.
	  *         This simply refers to the drawn image's default scaling relative to its source resolution.
	  */
	def imageScaling: Vector2D
	/**
	  * @return Insets placed around the image
	  */
	def insets: StackInsets
	
	/**
	  * @return Whether image should be allowed to scale beyond it's original size
	  *         (while still respecting image source resolution)
	  */
	def allowUpscaling: Boolean
	/**
	  * @return Whether this label should use lower priority size constraints
	  */
	def useLowPrioritySize: Boolean
	
	
	// IMPLEMENTED	------------------------
	
	override def calculatedStackSize = {
		val imageSize = visualImageSize
		// Applies the maximum size
		val raw = {
			if (allowUpscaling)
				StackSize.downscaling(imageSize.ceil, imageSize / imageScaling)
			else
				StackSize.downscaling(imageSize.ceil)
		}
		// Determines the correct priority to use
		val prioritized = {
			if (useLowPrioritySize) {
				if (allowUpscaling && raw.mayExpand)
					raw.lowPriority
				else
					raw.shrinking
			}
			else
				raw
		}
		// Adds insets
		prioritized + insets
	}
}
