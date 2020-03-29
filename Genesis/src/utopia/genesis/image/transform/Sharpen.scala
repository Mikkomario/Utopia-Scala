package utopia.genesis.image.transform

import utopia.genesis.shape.shape2D.Size

object Sharpen
{
	private def makeKernel(intensity: Float) = Array[Float](
		0.0f, -intensity / 5, 0.0f,
		-intensity / 5, intensity, -intensity / 5,
		0.0f, -intensity / 5, 0.0f
	)
}

/**
  * This image transformation sharpens an image
  * @author Mikko Hilpinen
  * @since 16.6.2019, v2.1+
  * @param intensity The level of sharpening intensity (default = 5)
  */
case class Sharpen(intensity: Double = 5) extends ConvolveTransform(Sharpen.makeKernel(intensity.toFloat), Size.square(3))
{
	/**
	  * @param mod Intensity modifier
	  * @return A multiplied version of this transform
	  */
	def *(mod: Double) = Sharpen(intensity * mod)
	/**
	  * @param div Intensity divider
	  * @return A divided version of this transform
	  */
	def /(div: Double) = Sharpen(intensity / div)
}
