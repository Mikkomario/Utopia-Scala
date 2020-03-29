package utopia.reflection.component.drawing.immutable

import utopia.genesis.image.Image
import utopia.reflection.component.drawing.template
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.shape.{Alignment, StackInsets}

/**
  * An immutable implementation of the image drawer
  * @author Mikko Hilpinen
  * @since 25.3.2020, v1
  */
class ImageDrawer(override val image: Image, override val insets: StackInsets = StackInsets.any,
				  override val alignment: Alignment = Alignment.Center, override val drawLevel: DrawLevel = Normal,
				  override val useUpscaling: Boolean = true) extends template.ImageDrawer
