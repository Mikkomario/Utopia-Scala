package utopia.genesis.shape.path

import utopia.genesis.animation.Animation

object SPath
{
	/**
	  * The steep s-path that follows the tanh-function
	  */
	val sharp = SPath(7.5, 1.9978)
	
	/**
	  * A version of the s-path that increases faster at the edges
	  */
	val default = SPath(5, 1.9735)
	
	/**
	  * A smoother version of the s-path with even faster increase at the edges
	  */
	val smooth = SPath(4, 1.928)
	
	/**
	  * A smoother version of the s-path with even faster increase at the edges
	  */
	val verySmooth = SPath(3, 1.81)
}

/**
  * This path traverses from (0, 0) to (1, 1), increasing smoothly in s-like curve. The increase is greatest at the
  * center (0.5 X) and lowest at the edges (0 and 1 X).
  * @author Mikko Hilpinen
  * @since 16.6.2020, v2.3
  */
case class SPath private(modifier: Double, divider: Double) extends Animation[Double]
{
	// Y = tanh(nX - 1/2n) / d + 0.5
	// Where n and d are static modifiers used for affecting curvature
	override def apply(progress: Double) = math.tanh(modifier * progress - modifier / 2.0) / divider + 0.5
}
