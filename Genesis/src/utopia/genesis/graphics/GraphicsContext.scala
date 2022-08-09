package utopia.genesis.graphics

import utopia.paradigm.transform.{AffineTransformable, LinearTransformable}
import utopia.paradigm.shape.shape3d.Matrix3D

/**
  * Provides read access to graphics related settings
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
// TODO: Remove this class
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
