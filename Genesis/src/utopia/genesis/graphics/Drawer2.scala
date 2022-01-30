package utopia.genesis.graphics

import utopia.genesis.shape.shape2D.{Bounds, Polygonic}
import utopia.genesis.shape.shape2D.transform.{AffineTransformable, LinearTransformable}

/**
  * Used for drawing. These drawer instances are available only for a limited time, after which they are closed.
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
// TODO: Remove this class
trait Drawer2 extends LinearTransformable[Drawer2] with AffineTransformable[Drawer2] with AutoCloseable
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return The graphics instance used by this drawer
	  */
	protected def graphics: ClosingGraphics
	
	/**
	  * @return Clipping bounds used in this drawer. The bounds are relative to this drawer's transformation system.
	  */
	def clipBounds: Bounds
	
	/**
	  * @param clippingArea A new clipping area to apply (overwriting the existing clipping area, if possible).
	  *                     The clipping area should be relative
	  *                     to this drawer's current transformation.
	  * @return A copy of this drawer that uses the specified clipping area.
	  */
	def withClip(clippingArea: Polygonic): Drawer2
	
	
	// TODO: Add draw functions
	
	
	// IMPLEMENTED  --------------------------
	
	override def close() = graphics.close()
}
