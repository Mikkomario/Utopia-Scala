package utopia.reach.component.visualization

import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.animation.{Animator, AnimatorInstruction}
import utopia.paradigm.animation.Animation

import scala.concurrent.duration.Duration

/**
  * Used for animating linear progress
  * @author Mikko Hilpinen
  * @since 12.02.2025, v1.6
  */
object ProgressAnimator
{
	/**
	  * Creates a new (smooth) progress animator
	  * @param progressPointer A pointer that contains the raw progress value
	  * @param drawAreaWidthPointer A pointer that contains the width of the drawn area
	  * @param animationDuration Duration of a single progress change animation (default = 0.2 seconds)
	  * @param maxJumpWithoutAnimationDistance Maximum progress distance
	  *                                        that may be advanced without a separate animation (default = 2 px)
	  * @param activeFlag A flag that contains true while this animator is active (default = always true)
	  * @return
	  */
	def apply(progressPointer: Changing[Double], drawAreaWidthPointer: Changing[Double],
	          animationDuration: Duration = 0.2.seconds, maxJumpWithoutAnimationDistance: Double = 2.0,
	          activeFlag: Flag = AlwaysTrue): Animator[Double] =
	{
		val isInstant = animationDuration <= Duration.Zero
		
		// Contains maximum amount of progress that may be jumped without an animation
		lazy val maxJumpProgressPointer =
			drawAreaWidthPointer.map { width => maxJumpWithoutAnimationDistance / width }
		
		// Used for directing the animator to visualize progress changes
		val animatorInstructionPointer = progressPointer.incrementalMap(AnimatorInstruction.fixed) { (_, event) =>
			// Case: The change in progress is so small that no animation is needed
			if (isInstant || event.values.merge { _ - _ }.abs < maxJumpProgressPointer.value)
				AnimatorInstruction.fixed(event.newValue)
			// Case: Significant change in progress => Animates the change
			else
				AnimatorInstruction(
					Animation.progress(event.oldValue, event.newValue).projectileCurved.over(animationDuration))
		}
		
		new Animator(animatorInstructionPointer, activeFlag = activeFlag)
	}
}
