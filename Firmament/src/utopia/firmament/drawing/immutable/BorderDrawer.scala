package utopia.firmament.drawing.immutable

import utopia.firmament.model.Border
import utopia.firmament.drawing.template.{BorderDrawerLike, DrawLevel}
import utopia.firmament.drawing.template.DrawLevel.Normal

/**
  * This custom drawer draws a set of borders inside the component without affecting component layout
  * @author Mikko Hilpinen
  * @since 5.5.2019, Reflection v1+
  */
case class BorderDrawer(override val border: Border, drawLevel: DrawLevel = Normal) extends BorderDrawerLike
