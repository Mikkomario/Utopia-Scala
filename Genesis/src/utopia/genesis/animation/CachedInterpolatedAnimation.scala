package utopia.genesis.animation

/**
  * This version of interpolated animation caches the results, which may be useful when animation contains
  * transformations too slow for real-time graphics. This implementation uses more memory, of course.
  * @author Mikko Hilpinen
  * @since 11.8.2019, v2.1+
  * @param original The original animation
  * @param intervalCount Amount of cached intervals / states
  * @tparam A Type of animation result
  */
class CachedInterpolatedAnimation[+A](original: Animation[A], val intervalCount: Int) extends Animation[A]
{
	// ATTRIBUTES	---------------------
	
	private val intervals = (1 to intervalCount).map { i => original(i.toDouble / (intervalCount + 1)) }
	
	
	// IMPLEMENTED	---------------------
	
	override def apply(progress: Double) = intervals((progress * intervalCount).toInt max 0 min (intervalCount - 1))
}
