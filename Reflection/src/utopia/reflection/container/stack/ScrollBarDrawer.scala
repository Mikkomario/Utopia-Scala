package utopia.reflection.container.stack

import utopia.genesis.shape.Axis2D
import utopia.genesis.util.Drawer
import utopia.reflection.shape.ScrollBarBounds

/**
  * These drawers draw scroll bars
  * @author Mikko Hilpinen
  * @since 30.4.2019, v1+
  */
trait ScrollBarDrawer
{
	/**
	  * Draws scroll bar
	  * @param drawer The drawer used
	  * @param barBounds The bounds for the scroll bar & area
	  * @param barDirection The direction of the scroll bar
	  */
	def draw(drawer: Drawer, barBounds: ScrollBarBounds, barDirection: Axis2D): Unit
}