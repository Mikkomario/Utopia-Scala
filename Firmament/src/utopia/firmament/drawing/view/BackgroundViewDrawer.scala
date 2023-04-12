package utopia.firmament.drawing.view

import utopia.flow.view.immutable.View
import utopia.paradigm.color.Color
import utopia.reflection.component.drawing.template.BackgroundDrawerLike
import utopia.reflection.component.drawing.template.DrawLevel.Background

/**
  * A pointer-based background drawer
  * @author Mikko Hilpinen
  * @since 15.11.2020, Reflection v2
  */
case class BackgroundViewDrawer(backgroundPointer: View[Color]) extends BackgroundDrawerLike
{
	override def drawLevel = Background
	
	override def color = backgroundPointer.value
}
