package utopia.genesis.animation.transform

import utopia.genesis.animation.Animation

/**
  * This animation uses an animated transform in addition to standard animation
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
trait AnimationWithTransform[Origin, +Reflection] extends Animation[Reflection]
{
	// ABSTRACT	-------------------------------
	
	/**
	  * @param progress Animation progress
	  * @return The raw state of this animation (state before applying transformation) at specified animation progress
	  */
	def raw(progress: Double): Origin
	
	/**
	  * @return Transform applied over the raw animation
	  */
	def transform: AnimatedTransform[Origin, Reflection]
	
	
	// IMPLEMENTED	---------------------------
	
	override def apply(progress: Double) = transform(raw(progress), progress)
}

object AnimationWithTransform
{
	// OTHER	-------------------------------
	
	/**
	  * Combines an animation with an animated transformation
	  * @param animation An animation
	  * @param transform An animated transformation
	  * @tparam Origin Type of original animation
	  * @tparam Reflection Type of transformation result
	  * @return A new animation that also applies the animated transformation
	  */
	def wrap[Origin, Reflection](animation: Animation[Origin],
								 transform: AnimatedTransform[Origin, Reflection]): AnimationWithTransform[Origin, Reflection] =
		new AnimationWithTransformWrapper(animation, transform)
	
	
	// NESTED	-------------------------------
	
	private class AnimationWithTransformWrapper[Origin, +Reflection](
		animation: Animation[Origin], override val transform: AnimatedTransform[Origin, Reflection])
		extends AnimationWithTransform[Origin, Reflection]
	{
		override def raw(progress: Double) = animation(progress)
	}
}