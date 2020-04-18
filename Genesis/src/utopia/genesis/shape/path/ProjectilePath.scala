package utopia.genesis.shape.path

import utopia.genesis.animation.Animation

/**
  * This path increases faster in the beginning and slower at the end. It is like a projectile's vertical coordinate
  * when thrown. Provides reasonable results between 0 and 1
  * @author Mikko Hilpinen
  * @since 17.4.2020, v2.2.1
  * @param curvatureModifier A modifier applied to curve shape. [0, 1[. 0 Is maximum curvature where the highest
  *                          point in the path resides at 1. The closer this value is to 1, the more this path looks like
  *                          a linear path.
  * @param end The value provided by this path at progress 1.0 (default = 1.0)
  */
case class ProjectilePath(curvatureModifier: Double = 0.0, end: Double = 1.0) extends Animation[Double]
{
	// IMPLEMENTED	----------------------------
	
	// Uses function y = (2x - (0.01 - c) * x^2) / (1 + 100c)
	// Where c is a curvature modifier [0, 1[ and both x and y are percentages [0, 100]
	def apply(progress: Double) =
	{
		val progressPercent = progress * 100
		val resultPercent = (2 * progressPercent - (0.01 - curvatureModifier) * math.pow(progressPercent, 2)) /
			(1 + curvatureModifier * 100)
		resultPercent / 100.0 * end
	}
	
	
	// OTHER	--------------------------------
	
	/**
	  * Modifies the "height" (meaning the produced value) of this path
	  * @param modifier "Height" modifier
	  * @return A copy of this path that has its output values modified by 'modifier'
	  */
	def *(modifier: Double) = copy(end = end * modifier)
}