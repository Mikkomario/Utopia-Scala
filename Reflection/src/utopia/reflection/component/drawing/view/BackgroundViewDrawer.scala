package utopia.reflection.component.drawing.view

import utopia.flow.datastructure.template.Viewable
import utopia.genesis.color.Color
import utopia.reflection.component.drawing.template.BackgroundDrawerLike
import utopia.reflection.component.drawing.template.DrawLevel.Background

/**
  * A pointer-based background drawer
  * @author Mikko Hilpinen
  * @since 15.11.2020, v2
  */
case class BackgroundViewDrawer(backgroundPointer: Viewable[Color]) extends BackgroundDrawerLike
{
	override def drawLevel = Background
	
	override def color = backgroundPointer.value
}
