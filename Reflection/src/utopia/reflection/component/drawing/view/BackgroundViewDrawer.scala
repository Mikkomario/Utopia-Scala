package utopia.reflection.component.drawing.view

import utopia.flow.datastructure.template.Viewable
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Background

/**
  * A pointer-based background drawer
  * @author Mikko Hilpinen
  * @since 15.11.2020, v2
  */
case class BackgroundViewDrawer(backgroundPointer: Viewable[Color]) extends CustomDrawer
{
	override def drawLevel = Background
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		if (bounds.size.isPositive)
			drawer.onlyFill(backgroundPointer.value).draw(bounds)
	}
}
