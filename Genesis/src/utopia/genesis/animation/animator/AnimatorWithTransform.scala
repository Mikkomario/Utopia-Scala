package utopia.genesis.animation.animator

import utopia.genesis.animation.transform.AnimatedTransform

/**
  * This animator applies an animated transformation over the original item. The original item may be static or animated.
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
trait AnimatorWithTransform[Origin, Reflection] extends Animator[Reflection]
{
	// ABSTRACT	----------------------
	
	/**
	  * @param progress Current animation progress [0, 1]
	  * @return The original item (before transformation is applied)
	  */
	def original(progress: Double): Origin
	/**
	  * @return Transformation applied over the item
	  */
	def transform: AnimatedTransform[Origin, Reflection]
	
	
	// IMPLEMENTED	-----------------
	
	override protected def apply(progress: Double) = transform(original(progress), progress)
}
