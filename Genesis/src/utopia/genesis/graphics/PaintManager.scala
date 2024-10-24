package utopia.genesis.graphics

import utopia.genesis.graphics.Priority.Normal
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D

/**
  * A common trait for classes which manage and optimize (component) painting
  * @author Mikko Hilpinen
  * @since 25.11.2020 in Reach v0.1 - Moved to Genesis 6.2.2024 at v4.0
  */
trait PaintManager
{
	// ABSTRACT	------------------------------
	
	/**
	  * Paints the managed region using the specified drawer
	  * @param drawer A drawer to use
	  */
	def paintWith(drawer: Drawer): Unit
	
	/*
	/**
	  * Paints (a portion of) the managed region. Doesn't require and a buffer or state update, unless that's necessary
	  * to perform the actual drawing.
	  * @param region Region that should be painted. None if the whole managed region should be painted (default).
	  * @param priority Requested priority for handling this paint call. Higher priority requests should result in
	  *                 faster / prioritized painting, although the actual effect of this parameter is
	  *                 implementation dependent.
	  */
	def paint(region: Option[Bounds] = None, priority: Priority2 = Normal): Unit
	*/
	
	/**
	  * Requests a repaint of (a portion of) the managed region
	  * @param region   Targeted sub-region within the managed region.
	  *                 None if the whole region should be repainted (default).
	  * @param priority Requested priority for handling this repaint call. Higher priority requests should result in
	  *                 faster / prioritized repainting, although the actual effect of this parameter is
	  *                 implementation dependent.
	  */
	def repaint(region: Option[Bounds] = None, priority: Priority = Normal): Unit
	
	/**
	  * Shifts a sub-region in the managed area to a new location
	  * @param originalArea The targeted sub-region
	  * @param transition   Amount of translation applied to the region
	  */
	def shift(originalArea: Bounds, transition: Vector2D): Unit
	
	/*
	/**
	  * @param area A sub-region of the painted / managed region
	  * @return The average shade (dark or light) of the targeted area
	  */
	def averageShadeOf(area: Bounds): ColorShade
	*/
	
	// OTHER	-------------------------------
	
	/**
	  * Requests a repaint of a portion of the managed region
	  * @param region   Targeted sub-region within the managed region
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
