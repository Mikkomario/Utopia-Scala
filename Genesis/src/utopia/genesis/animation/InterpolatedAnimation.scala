package utopia.genesis.animation

/**
  * Interpolates an animation
  * @author Mikko Hilpinen
  * @since 11.8.2019, v2.1+
  * @param original The original animation
  * @param intervalCount Amount of different states or intervals in the new animation
  * @tparam A Type of animation result
  */
case class InterpolatedAnimation[+A](original: Animation[A], intervalCount: Int) extends Animation[A]
{
	// COMPUTED	-------------------------
	
	/**
	  * @return A cached version of this animation
	  */
	def cached = new CachedInterpolatedAnimation(original, intervalCount)
	
	
	// IMPLEMENTED	---------------------
	
	override def apply(progress: Double) = original((progress * intervalCount).ceil / (intervalCount + 1.0))
}
