package utopia.paradigm.path

import utopia.flow.operator.combine.LinearScalable
import utopia.flow.util.Mutate
import utopia.paradigm.transform.Adjustment

object ProjectilePath
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * A fully curved path (i.e. Y=X&#94;2)
	  */
	lazy val curved = apply()
	/**
	  * A fully linear path (i.e. Y=X)
	  */
	lazy val linear = apply(1.0)
}

/**
  * A path which starts from (0,0) and advances to (1,'end') in a projectile-like fashion.
  * I.e. advances faster at first, then reaches 1 slower at the end.
  * @author Mikko Hilpinen
  * @since 11.06.2025, v1.7.3
  * @param linearity A value between 0 and 1, which determines how linear or curved this path is.
  *                  0 is totally curved (i.e. Y=X&#94;2, default). 1 if totally linear (i.e. Y=X).
  * @param end The Y value yielded at X=1.
  */
case class ProjectilePath(linearity: Double = 0.0, end: Double = 1.0)
	extends Path[Double] with LinearScalable[ProjectilePath]
{
	// ATTRIBUTES   -------------------------
	
	override val start: Double = 0.0
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return The curvature modifier of this path.
	  *         0 if fully linear. 1 if fully curved.
	  */
	def curvature = 1 - linearity
	
	/**
	  * @param adj Implicit adjustment to apply
	  * @return A more curved version of this path
	  */
	def moreCurved(implicit adj: Adjustment) = mapLinearity { _ * adj(-1) }
	/**
	  * @param adj Implicit adjustment to apply
	  * @return A more linear version of this path
	  */
	def moreLinear(implicit adj: Adjustment) = mapCurvature { _ * adj(-1) }
	
	
	// IMPLEMENTED    -----------------------
	
	override def self: ProjectilePath = this
	
	override def apply(progress: Double): Double = {
		// Takes a weighed average between a curved and a linear path
		val reverseProgress = 1 - progress
		val curved = math.pow(reverseProgress, 2)
		val linear = reverseProgress
		val defaultResult = (1 - (curved * curvature + linear * linearity)) * end
		
		if (progress >= 0 && progress <= 1)
			(defaultResult max 0) min end
		else
			defaultResult
	}
	
	override def *(mod: Double): ProjectilePath = copy(end = end * mod)
	
	
	// OTHER    --------------------------
	
	def withLinearity(linearity: Double) = copy(linearity = linearity)
	def mapLinearity(f: Mutate[Double]) = withLinearity(f(linearity))
	
	def withCurvature(curvature: Double) = withLinearity(1 - curvature)
	def mapCurvature(f: Mutate[Double]) = withCurvature(f(curvature))
}
