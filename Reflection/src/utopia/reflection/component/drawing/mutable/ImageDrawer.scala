package utopia.reflection.component.drawing.mutable

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.image.Image
import utopia.reflection.component.drawing.template
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.shape.{Alignment, StackInsets}

/**
  * A mutable, pointer-based implementation of the image drawer trait
  * @author Mikko Hilpinen
  * @since 25.3.2020, v1
  */
class ImageDrawer(val imagePointer: PointerWithEvents[Image],
				  val insetsPointer: PointerWithEvents[StackInsets] = new PointerWithEvents[StackInsets](StackInsets.any),
				  val alignmentPointer: PointerWithEvents[Alignment] = new PointerWithEvents[Alignment](Alignment.Center),
				  override val drawLevel: DrawLevel = Normal, override val useUpscaling: Boolean = true) extends template.ImageDrawer
{
	// IMPLEMENTED	------------------------
	
	override def image = imagePointer.value
	def image_=(newImage: Image) = imagePointer.value = newImage
	
	override def insets = insetsPointer.value
	def insets_=(newInsets: StackInsets) = insetsPointer.value = newInsets
	
	override def alignment = alignmentPointer.value
	def alignment_=(newAlignment: Alignment) = alignmentPointer.value = newAlignment
}
