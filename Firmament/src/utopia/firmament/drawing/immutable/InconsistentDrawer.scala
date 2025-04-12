package utopia.firmament.drawing.immutable

import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.view.immutable.View
import utopia.genesis.graphics.{DrawLevel, Drawer}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * A custom drawer that wraps another drawer, utilizing it only when a certain condition is met
  * @author Mikko Hilpinen
  * @since 11.04.2025, v1.4.1
  */
class InconsistentDrawer(wrapped: CustomDrawer, drawCondition: View[Boolean]) extends CustomDrawer
{
	override def opaque: Boolean = wrapped.opaque && drawCondition.value
	override def drawLevel: DrawLevel = wrapped.drawLevel
	
	override def draw(drawer: Drawer, bounds: Bounds): Unit = {
		if (drawCondition.value)
			wrapped.draw(drawer, bounds)
	}
}
