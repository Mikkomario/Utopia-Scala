package utopia.firmament.drawing.immutable

import utopia.firmament.drawing.template.BackgroundDrawerLike
import utopia.genesis.graphics.DrawLevel
import utopia.genesis.graphics.DrawLevel.Background
import utopia.paradigm.color.Color

/**
  * A custom drawer that draws a background for the targeted component
  * @author Mikko Hilpine
  * @since 28.2.2020, Reflection v1
  */
case class BackgroundDrawer(override val color: Color, override val drawLevel: DrawLevel = Background)
	extends BackgroundDrawerLike
