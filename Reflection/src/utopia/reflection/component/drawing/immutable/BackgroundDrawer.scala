package utopia.reflection.component.drawing.immutable

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel.Background
import utopia.reflection.component.drawing.template.{CustomDrawer, DrawLevel}

/**
  * A custom drawer that draws a background for the targeted component
  * @author Mikko Hilpine
  * @since 28.2.2020, v1
  */
class BackgroundDrawer(color: Color, override val drawLevel: DrawLevel = Background) extends CustomDrawer
{
	override def draw(drawer: Drawer, bounds: Bounds) = drawer.onlyFill(color).draw(bounds)
}
