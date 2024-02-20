package utopia.firmament.drawing.immutable

import utopia.firmament.drawing.template.BorderDrawerLike
import utopia.firmament.model.Border
import utopia.genesis.graphics.DrawLevel2
import utopia.genesis.graphics.DrawLevel2.Normal

/**
  * This custom drawer draws a set of borders inside the component without affecting component layout
  * @author Mikko Hilpinen
  * @since 5.5.2019, Reflection v1+
  */
case class BorderDrawer(override val border: Border, drawLevel: DrawLevel2 = Normal) extends BorderDrawerLike
