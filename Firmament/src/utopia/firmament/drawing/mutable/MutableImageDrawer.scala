package utopia.firmament.drawing.mutable

import utopia.genesis.image.Image
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.drawing.template
import utopia.genesis.graphics.DrawLevel2
import utopia.genesis.graphics.DrawLevel2.Normal
import utopia.firmament.model.stack.StackInsets

/**
  * A mutable, pointer-based implementation of the image drawer trait
  * @author Mikko Hilpinen
  * @since 25.3.2020, Reflection v1
  */
class MutableImageDrawer(initialImage: Image, initialInsets: StackInsets = StackInsets.any,
						 initialAlignment: Alignment = Alignment.Center, override val drawLevel: DrawLevel2 = Normal,
						 override val useUpscaling: Boolean = true) extends template.ImageDrawerLike
{
	// ATTRIBUTES	---------------------------
	
	var image = initialImage
	var insets = initialInsets
	var alignment = initialAlignment
}
