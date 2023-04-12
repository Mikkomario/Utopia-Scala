package utopia.firmament.drawing.mutable

import utopia.genesis.image.Image
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.component.drawing.template
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.shape.stack.StackInsets

/**
  * A mutable, pointer-based implementation of the image drawer trait
  * @author Mikko Hilpinen
  * @since 25.3.2020, Reflection v1
  */
class MutableImageDrawer(initialImage: Image, initialInsets: StackInsets = StackInsets.any,
						 initialAlignment: Alignment = Alignment.Center, override val drawLevel: DrawLevel = Normal,
						 override val useUpscaling: Boolean = true) extends template.ImageDrawerLike
{
	// ATTRIBUTES	---------------------------
	
	var image = initialImage
	var insets = initialInsets
	var alignment = initialAlignment
}
