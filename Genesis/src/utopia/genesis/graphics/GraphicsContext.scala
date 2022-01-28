package utopia.genesis.graphics

import utopia.genesis.shape.shape2D.transform.{AffineTransformable, LinearTransformable}
import utopia.genesis.shape.shape3D.Matrix3D

/**
  * Provides read access to graphics related settings
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
// TODO: Consider changing this trait into a wrapper class for a LazyGraphics instance
trait GraphicsContext
	extends LinearTransformable[GraphicsContext] with AffineTransformable[GraphicsContext]
{
	/**
	  * @return Transformation applied by this context
	  */
	def transformation: Matrix3D
	
	/**
	  * Closes the currently open graphics resource from this level and below
	  */
	def closeCurrent(): Unit
	
	// TODO: Add font render context access
}
