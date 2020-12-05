package utopia.reflection.util

import utopia.genesis.shape.shape2D.{Bounds, Vector2D}
import utopia.genesis.util.Drawer
import utopia.reflection.color.ColorShadeVariant
import utopia.reflection.util.Priority.Normal

/**
  * A common trait for classes which manage and optimize component painting
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2
  */
trait PaintManager
{
	// ABSTRACT	------------------------------
	
	/**
	  * Paints the managed region using the specified drawer
	  * @param drawer A drawer to use
	  */
	def paintWith(drawer: Drawer): Unit
	
	/**
	  * Requests a repaint of (a portion of) the managed region
	  * @param region Targeted sub-region within the managed region.
	  *               None if the whole region should be repainted (default).
	  * @param priority Requested priority for handling this repaint call. Higher priority requests should result in
	  *                 faster / prioritized repainting, although the actual effect of this parameter is
	  *                 implementation dependent.
	  */
	def repaint(region: Option[Bounds] = None, priority: Priority = Normal): Unit
	
	/**
	  * Shifts a sub-region in the managed area to a new location
	  * @param originalArea The targeted sub-region
	  * @param transition Amount of translation applied to the region
	  */
	def shift(originalArea: Bounds, transition: Vector2D): Unit
	
	/**
	  * @param area A sub-region of the painted / managed region
	  * @return The average shade (dark or light) of the targeted area
	  */
	def averageShadeOf(area: Bounds): ColorShadeVariant
	
	
	// OTHER	-------------------------------
	
	/**
	  * Requests a repaint of a portion of the managed region
	  * @param region Targeted sub-region within the managed region
	  * @param priority Requested priority for handling this repaint call. Higher priority requests should result in
	  *                 faster / prioritized repainting, although the actual effect of this parameter is
	  *                 implementation dependent.
	  */
	def repaintRegion(region: Bounds, priority: Priority = Normal) = repaint(Some(region), priority)
	
	/**
	  * Requests a repaint of the managed region
	  * @param priority Requested priority for handling this repaint call. Higher priority requests should result in
	  *                 faster / prioritized repainting, although the actual effect of this parameter is
	  *                 implementation dependent.
	  */
	def repaintAll(priority: Priority = Normal) = repaint(None, priority)
}
