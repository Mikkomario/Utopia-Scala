package utopia.firmament.drawing.mutable

import utopia.firmament.drawing.immutable.ImageDrawer
import utopia.firmament.model.stack.StackInsets
import utopia.flow.view.mutable.Pointer
import utopia.genesis.graphics.DrawLevel
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.image.ImageView
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.Matrix2D

object MutableImageDrawer
{
	def apply(image: ImageView, transformation: Option[Matrix2D] = None, insets: StackInsets = StackInsets.any,
	          alignment: Alignment = Center, drawLevel: DrawLevel = Normal, upscales: Boolean) =
		new MutableImageDrawer(Pointer(image), Pointer(transformation), Pointer(insets), Pointer(alignment), drawLevel,
			upscales)
}

/**
  * A mutable, pointer-based implementation of the image drawer trait
  * @author Mikko Hilpinen
  * @since 25.3.2020, Reflection v1
  */
class MutableImageDrawer(imagePointer: Pointer[ImageView],
                         transformationPointer: Pointer[Option[Matrix2D]] = Pointer.empty,
                         insetsPointer: Pointer[StackInsets] = Pointer(StackInsets.any),
                         alignmentPointer: Pointer[Alignment] = Pointer(Center),
                         override val drawLevel: DrawLevel = Normal,
                         override val useUpscaling: Boolean = true)
	extends ImageDrawer
{
	// IMPLEMENTED  ---------------------------
	
	override def image: ImageView = imagePointer.value
	def image_=(img: ImageView) = imagePointer.value = img
	
	override def transformation: Option[Matrix2D] = transformationPointer.value
	def transformation_=(t: Option[Matrix2D]) = transformationPointer.value = t
	
	override def insets: StackInsets = insetsPointer.value
	def insets_=(i: StackInsets) = insetsPointer.value = i
	
	override def alignment: Alignment = alignmentPointer.value
	def alignment_=(alignment: Alignment) = alignmentPointer.value = alignment
}
