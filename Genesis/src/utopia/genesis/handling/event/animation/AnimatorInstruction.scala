package utopia.genesis.handling.event.animation

import utopia.flow.collection.immutable.range.{HasInclusiveOrderedEnds, NumericSpan}
import utopia.flow.util.Mutate
import utopia.genesis.handling.event.animation.AnimatorInstruction.defaultClip
import utopia.paradigm.animation.TimedAnimation

import scala.concurrent.duration.Duration
import scala.math.Ordering.Double.TotalOrdering

object AnimatorInstruction
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * The default animation clip, which corresponds with an animation's standard input range 0 to 1.
	  */
	val defaultClip = NumericSpan(0.0, 1.0)
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param value Value to present
	  * @tparam A Type of the presented value
	  * @return An instruction to present a fixed (i.e. non-animated) value
	  */
	def fixed[A](value: A) = apply(TimedAnimation.fixed(value), NumericSpan.singleValue(1.0))
}

/**
  * Represents an instruction to an Animator instance to switch the played animation
  * @author Mikko Hilpinen
  * @since 22/02/2024, v4.0
  *
  * @constructor Creates a new instruction by wrapping an animation
  * @param animation The animation to play
  * @param clip The range of animation that should be played,
  *             where 0.0 is original animation beginning and 1.0 is original animation end.
  *             Default = 0.0 to 1.0 = full animation range.
  * @param continues Whether progress from the previous animation
  *                  should be preserved and carried over to this animation, if applicable.
  *                  Default = false = this animation starts from the beginning.
  * @param loops Whether the animation should be looped (i.e. started over once it finishes).
  *              Default = false = the animation should stop once completed.
  */
case class AnimatorInstruction[+A](animation: TimedAnimation[A], clip: NumericSpan[Double] = defaultClip,
                                   continues: Boolean = false, loops: Boolean = false)
	extends TimedAnimation[A] with HasInclusiveOrderedEnds[Double]
{
	// ATTRIBUTES   -----------------------
	
	override lazy val velocity = animation.velocity
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return True if the animation should stop once it finishes
	  */
	def stopsAtEnd = !loops
	
	/**
	  * @return True if the animation should not be animated
	  */
	def isStatic = clip.ends.isSymmetric || !animation.duration.isFinite
	/**
	  * @return True if the animation should be animated
	  */
	def animates = !isStatic
	
	/**
	  * @return Progress state where the animation should be fixed.
	  *         None if the animation shouldn't be static / fixed to any single value.
	  */
	def fixedState = {
		val v = clip.start
		if (v == clip.end) Some(v) else None
	}
	
	/**
	  * @return Copy of this instruction that carries over the progress from a previous animation, if applicable
	  */
	def continuing = if (continues) this else copy(continues = true)
	/**
	  * @return Copy of this instruction that starts the animation from beginning when switched from another animation
	  */
	def restarting = if (continues) copy(continues = false) else this
	
	/**
	  * @return Copy of this instruction that loops the animation
	  */
	def looping = if (loops) this else copy(loops = true)
	/**
	  * @return Copy of this instruction that only plays the animation once
	  */
	def once = if (loops) copy(loops = false) else this
	
	/**
	  * @return Copy of this instruction where the animation starts from its original beginning
	  */
	def fromBeginning = startingFrom(0.0)
	/**
	  * @return Copy of this instruction where the animation continues until its original end
	  */
	def untilEnd = endingAt(1.0)
	/**
	  * @return Copy of this instruction where the animation is fully played
	  */
	def full = withClip(defaultClip)
	
	
	// IMPLEMENTED  -----------------------
	
	override implicit def ordering: Ordering[Double] = TotalOrdering
	override def start: Double = clip.start
	override def end: Double = clip.end
	
	override def duration: Duration = animation.duration
	
	override def apply(progress: Double): A = animation(progress)
	override def apply(passedTime: Duration) = animation(passedTime)
	
	override def map[B](f: A => B) = copy(animation = animation.map(f))
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param newClip New "clip" (animation play range) to assign.
	  *                Here 0.0 represents the beginning of the original animation and 1.0 represents its end.
	  * @return Copy of this instruction that plays the specified animation range only
	  */
	def withClip(newClip: NumericSpan[Double]) =
		if (clip == newClip) this else copy(clip = newClip)
	/**
	  * @param f A mapping function applied to the played animation range
	  * @return Copy of this instruction with the mapped range
	  */
	def mapClip(f: Mutate[NumericSpan[Double]]) = withClip(f(clip))
	
	/**
	  * @param start New animation starting position [0,1]
	  * @return Copy of this instruction where the animation starts at the specified progress state.
	  */
	def startingFrom(start: Double) =
		mapClip { c => if (c.end <= start) NumericSpan.singleValue(start) else c.withStart(start) }
	/**
	  * @param end New animation ending position [0,1]
	  * @return Copy of this instruction where the animation ends at the specified progress state.
	  */
	def endingAt(end: Double) =
		mapClip { c => if (c.start >= end) NumericSpan.singleValue(end) else c.withEnd(end) }
	/**
	  * @param progress Displayed progress state [0,1]
	  * @return Copy of this instruction where the animation is static and only displays the specified progress state.
	  */
	def fixedTo(progress: Double) = withClip(NumericSpan.singleValue(progress))
}
