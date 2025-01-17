package utopia.reach.drawing

import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.view.immutable.View
import utopia.genesis.graphics.DrawLevel.Foreground
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer, StrokeSettings}
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * A custom drawer used for visualizing whether a component recognizes it's connected to the main stack hierarchy
  * @author Mikko Hilpinen
  * @since 16.01.2025, v1.5
  */
class HierarchyConnectionVisualizer(linkFlag: View[Boolean]) extends CustomDrawer
{
	// ATTRIBUTES   --------------------
	
	override val opaque: Boolean = false
	override val drawLevel: DrawLevel = Foreground
	
	private val dsP = linkFlag.mapValue { linked =>
		val color = if (linked) Color.yellow.lightened.withAlpha(0.15) else Color.blue.withAlpha(0.4)
		DrawSettings(color)(StrokeSettings(color.darkenedBy(2)))
	}
	
	
	// COMPUTED ------------------------
	
	private implicit def ds: DrawSettings = dsP.value
	
	
	// IMPLEMENTED  --------------------
	
	override def draw(drawer: Drawer, bounds: Bounds): Unit = drawer.draw(bounds)
}
