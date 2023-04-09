package utopia.reflection.component.drawing.template

import utopia.genesis.graphics.Drawer3
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.shape.ScrollBarBounds

/**
  * These drawers draw scroll bars
  * @author Mikko Hilpinen
  * @since 30.4.2019, v1+
  */
trait ScrollBarDrawerLike
{
	/**
	  * Draws scroll bar
	  * @param drawer The drawer used
	  * @param barBounds The bounds for the scroll bar & area
	  * @param barDirection The direction of the scroll bar
	  */
	def draw(drawer: Drawer3, barBounds: ScrollBarBounds, barDirection: Axis2D): Unit
}
