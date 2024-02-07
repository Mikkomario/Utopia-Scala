package utopia.genesis.handling.drawing

import utopia.genesis.graphics.Priority2
import utopia.genesis.graphics.Priority2.Normal
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * Common trait for items that are interested in Drawable repaint events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
trait RepaintListener
{
	/**
	  * This function should be called in order to trigger a repaint of a Drawable item
	  * @param item The item that should be drawn again
	  * @param subRegion A region relative to (and within) item's draw bounds, which should be drawn again.
	  *                  None if the item's whole draw bounds should be drawn again.
	  * @param priority Priority with which the item should be drawn
	  */
	def repaint(item: Drawable2, subRegion: Option[Bounds] = None, priority: Priority2 = Normal): Unit
}
