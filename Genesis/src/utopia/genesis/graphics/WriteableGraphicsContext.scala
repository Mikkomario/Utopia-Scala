package utopia.genesis.graphics

import utopia.genesis.shape.shape2D.transform.{AffineTransformable, LinearTransformable}

import java.awt.Graphics2D

/**
  * Provides read and write access to graphics related settings
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
trait WriteableGraphicsContext extends GraphicsContext
	with LinearTransformable[WriteableGraphicsContext] with AffineTransformable[WriteableGraphicsContext]
{
	/**
	  * @return A usable graphics instance. Care should be taken when providing access to this value since
	  *         it contains many mutable methods.
	  */
	def openGraphics: Graphics2D
}
