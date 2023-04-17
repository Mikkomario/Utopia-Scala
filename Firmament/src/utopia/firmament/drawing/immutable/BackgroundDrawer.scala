package utopia.firmament.drawing.immutable

import utopia.paradigm.color.Color
import utopia.firmament.drawing.template.DrawLevel.Background
import utopia.firmament.drawing.template.{BackgroundDrawerLike, DrawLevel}

/**
  * A custom drawer that draws a background for the targeted component
  * @author Mikko Hilpine
  * @since 28.2.2020, Reflection v1
  */
case class BackgroundDrawer(override val color: Color, override val drawLevel: DrawLevel = Background)
	extends BackgroundDrawerLike
