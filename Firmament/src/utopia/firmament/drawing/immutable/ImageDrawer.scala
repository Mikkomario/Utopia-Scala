package utopia.firmament.drawing.immutable

import utopia.genesis.image.Image
import utopia.firmament.drawing.template
import utopia.firmament.drawing.template.DrawLevel
import utopia.firmament.drawing.template.DrawLevel.Normal
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.StackInsets

/**
  * An immutable implementation of the image drawer
  * @author Mikko Hilpinen
  * @since 25.3.2020, Reflection v1
  */
case class ImageDrawer(override val image: Image, override val insets: StackInsets = StackInsets.any,
				  override val alignment: Alignment = Alignment.Center, override val drawLevel: DrawLevel = Normal,
				  override val useUpscaling: Boolean = true)
	extends template.ImageDrawerLike
