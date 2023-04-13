package utopia.firmament.drawing.view

import utopia.flow.view.immutable.View
import utopia.genesis.image.Image
import utopia.reflection.component.drawing.template.{DrawLevel, ImageDrawerLike}
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.StackInsets

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
	def withStaticStyle(imagePointer: View[Image], insets: StackInsets = StackInsets.any,
	                    alignment: Alignment = Alignment.Center, drawLevel: DrawLevel = Normal,
	                    useUpscaling: Boolean = true) =
		apply(imagePointer, View.fixed(insets), View.fixed(alignment), drawLevel, useUpscaling)
}

/**
  * A drawer that draws image content based on changing pointer values
  * @author Mikko Hilpinen
  * @since 21.10.2020, Reflection v2
  */
case class ImageViewDrawer(imagePointer: View[Image], insetsPointer: View[StackInsets] = View.fixed(StackInsets.any),
                           alignmentPointer: View[Alignment] = View.fixed(Alignment.Center),
                           override val drawLevel: DrawLevel = Normal,
                           override val useUpscaling: Boolean = true)
	extends ImageDrawerLike
{
	// IMPLEMENTED	------------------------
	
	override def image = imagePointer.value
	
	override def insets = insetsPointer.value
	
	override def alignment = alignmentPointer.value
}
