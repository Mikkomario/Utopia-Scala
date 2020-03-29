package utopia.genesis.image.transform

import utopia.genesis.shape.shape2D.Size

object Blur
{
	private def makeKernel(intensity: Float) =
	{
		val center = 1 - intensity * 0.8f
		val leftOver = 1 - center
		
		Array[Float](
			0.0f, leftOver / 4, 0.0f,
			leftOver / 4, center, leftOver / 4,
			0.0f, leftOver / 4, 0.0f
		)
	}
}

/**
  * This image transformation blurs an image
  * @author Mikko Hilpinen
  * @since 16.6.2019, v2.1+
  * @param intensity The intensity modifier for the blur [0, 1] where 0 means no blur (default = 1)
  */
case class Blur(intensity: Double = 1) extends ConvolveTransform(Blur.makeKernel(intensity.toFloat), Size.square(3))
