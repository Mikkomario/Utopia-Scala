package utopia.reflection.component.drawing.view

import utopia.flow.datastructure.immutable.View
import utopia.flow.datastructure.template.Viewable
import utopia.genesis.image.Image
import utopia.reflection.component.drawing.template.{DrawLevel, ImageDrawerLike}
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.StackInsets

object ImageViewDrawer
{
	/**
	  * Creates a new image drawer that only varies in drawn image content
	  * @param imagePointer Pointer to the changing image content
	  * @param insets Insets placed around the image (default = any, preferring 0)
	  * @param alignment Alignment used when placing the image (default = Center)
	  * @param drawLevel Draw level used (default = Normal)
	  * @param useUpscaling Whether image should be allowed to scale above its normal size
	  *                     (image resolution is still respected) (default = true)
	  * @return A new image drawer
	  */
	def withStaticStyle(imagePointer: Viewable[Image], insets: StackInsets = StackInsets.any,
						alignment: Alignment = Alignment.Center, drawLevel: DrawLevel = Normal,
						useUpscaling: Boolean = true) =
		apply(imagePointer, View(insets), View(alignment), drawLevel, useUpscaling)
}

/**
  * A drawer that draws image content based on changing pointer values
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  */
case class ImageViewDrawer(imagePointer: Viewable[Image], insetsPointer: Viewable[StackInsets],
						   alignmentPointer: Viewable[Alignment], override val drawLevel: DrawLevel = Normal,
						   override val useUpscaling: Boolean = true) extends ImageDrawerLike
{
	// IMPLEMENTED	------------------------
	
	override def image = imagePointer.value
	
	override def insets = insetsPointer.value
	
	override def alignment = alignmentPointer.value
}
