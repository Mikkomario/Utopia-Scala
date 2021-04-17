package utopia.reflection.component.drawing.immutable

import utopia.reflection.component.drawing.template.{BorderDrawerLike, DrawLevel}
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.shape.Border

/**
  * This custom drawer draws a set of borders inside the component without affecting component layout
  * @author Mikko Hilpinen
  * @since 5.5.2019, v1+
  */
case class BorderDrawer(override val border: Border, drawLevel: DrawLevel = Normal) extends BorderDrawerLike
