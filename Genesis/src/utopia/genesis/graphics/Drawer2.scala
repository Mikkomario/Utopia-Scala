package utopia.genesis.graphics

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.shape.shape2D.transform.{AffineTransformable, LinearTransformable}

/**
  * Used for drawing. These drawer instances are available only for a limited time, after which they are closed.
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
trait Drawer2 extends LinearTransformable[Drawer2] with AffineTransformable[Drawer2] with AutoCloseable
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Clipping bounds used in this drawer
	  */
	def clipBounds: Bounds
	
	/**
	  * @return Graphics context used in this drawer
	  */
	protected def writeContext: WriteableGraphicsContext
	
	// TODO: Add draw functions
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Graphics context used by this drawer
	  */
	def context: GraphicsContext = writeContext
	
	/**
	  * @return The graphics instance used in this drawer
	  */
	protected def graphics = writeContext.openGraphics
	
	
	// IMPLEMENTED  --------------------------
	
	override def close() = writeContext.closeCurrent()
}
