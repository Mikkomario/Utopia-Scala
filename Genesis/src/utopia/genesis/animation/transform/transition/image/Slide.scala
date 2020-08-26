package utopia.genesis.animation.transform.transition.image

import utopia.genesis.animation.transform.AnimatedTransform
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Direction2D

/**
  * Slides another image in and another out
  * @author Mikko Hilpinen
  * @since 23.8.2020, v2.3
  */
class Slide(direction: Direction2D) extends AnimatedTransform[(Image, Image), Image]
{
	override def apply(original: (Image, Image), progress: Double) =
	{
		if (progress <= 0.0)
			original._1
		else if (progress >= 1.0)
			original._2
		else
		{
			// TODO: Implement (the challenge is to get the two images to align and to handle the performance efficiently)
		}
		???
	}
}
