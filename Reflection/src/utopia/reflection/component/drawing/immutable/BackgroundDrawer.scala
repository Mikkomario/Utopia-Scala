package utopia.reflection.component.drawing.immutable

import utopia.paradigm.color.Color
import utopia.reflection.component.drawing.template.DrawLevel.Background
import utopia.reflection.component.drawing.template.{BackgroundDrawerLike, DrawLevel}

/**
  * A custom drawer that draws a background for the targeted component
  * @author Mikko Hilpine
  * @since 28.2.2020, v1
  */
case class BackgroundDrawer(override val color: Color, override val drawLevel: DrawLevel = Background)
	extends BackgroundDrawerLike
