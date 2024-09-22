package utopia.genesis.handling.event.animation

import utopia.flow.util.EitherExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue}
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.{CopyOnDemand, ResettableFlag, SettableFlag}
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper, Flag}
import utopia.genesis.handling.action.Actor
import utopia.genesis.handling.event.animation.AnimationEvent.{Completed, Paused, Started}

import scala.concurrent.duration.FiniteDuration

/**
  * A pointer-based class that advances an animation over time, according to the instructions it receives.
  * @author Mikko Hilpinen
  * @since 22/02/2024, v4.0
  */
class Animator[+A](instructionPointer: Changing[AnimatorInstruction[A]], activeFlag: Flag = AlwaysTrue)
	extends Actor with ChangingWrapper[A]
{
	// ATTRIBUTES   ---------------------------
	
	// Contains the fixed target frame, if/when specified
	private val staticStatePointer = instructionPointer.map { _.fixedState }
	// Contains true while the animation is allowed to play.
	// Contains false if this animator should display a fixed state.
	private val isAnimationFlag: Flag = staticStatePointer.map { _.isEmpty }
	
	// Left if the animation never finishes or has already finished
	// Right if the animation may finish
	//      Right:Right if the animation finishes once only
	//      Right:Left if the animation may finish multiple times
	private val _finishedFlag = {
		if (isAnimationFlag.isAlwaysFalse)
			Left(AlwaysTrue)
		else
			instructionPointer.fixedValue match {
				case Some(instruction) =>
					if (instruction.loops)
						Left(AlwaysFalse)
					else
						Right(Right(SettableFlag()))
				case None => Right(Left(ResettableFlag()))
			}
	}
	/**
	  * A flag that contains true when the animation has finished and stopped.
	  * NB: Still contains false if the animation is paused.
	  */
	val finishedFlag = _finishedFlag match {
		case Right(flag) => flag.either
		case Left(flag) => flag
	}
	private val notFinishedFlag = !finishedFlag
	
	private val animationPointer = instructionPointer.map { _.animation }
	private val velocityPointer = animationPointer.map { _.velocity }
	private val hasVelocityFlag: Flag = velocityPointer.map { _.nonZero }
	
	// Contains true while the animation is paused (either via activeFlag or by setting animation velocity to zero)
	private val notPausedFlag = activeFlag && hasVelocityFlag
	// There are multiple conditions that must be met in order for action events to be of use:
	//      1) Possible custom condition
	//      2) Animation must not be stopped after completion
	//      3) Animation must have velocity (progress)
	//      4) Animation must not be fixed to a single frame
	override val handleCondition: Flag = notPausedFlag && notFinishedFlag && isAnimationFlag
	
	/**
	  * A pointer that contains the current animation progress. Between 0 and 1.
	  */
	protected val progressPointer = Volatile.eventful(instructionPointer.value.start)
	
	// NB: Needs to be updated manually because of the animation-switching
	// (progress and animation may need to be updated simultaneously)
	private val animatedPointer = CopyOnDemand(View { animationPointer.value(currentProgress) })
	
	/**
	  * A handler to which this animator delivers its animation events
	  */
	val handler = AnimationHandler()
	
	
	// INITIAL CODE ---------------------------
	
	// Tracks the "active" flag while event-generation is appropriate and until animation finishes
	notPausedFlag.addListenerWhile(handler.handleCondition && notFinishedFlag) { event =>
		// Case: Animation resumes after being paused => Fires an event
		if (event.newValue)
			handler.onAnimationEvent(Paused(currentProgress))
		// Case: Animation pauses => Fires an event
		else
			handler.onAnimationEvent(Started(currentProgress))
	}
	
	progressPointer.addContinuousAnyChangeListener { animatedPointer.update() }
	
	// Updates the state when instructions are updated
	instructionPointer.addContinuousListener { event =>
		// Case: Played animation changes or the targeted animation range changes => Reacts
		val animationChanged = event.values.isAsymmetricBy { _.animation }
		if (animationChanged || event.values.isAsymmetricBy { _.clip }) {
			val newInstruction = event.newValue
			newInstruction.fixedState match {
				// Case: Fixed to a frame => Sets the progress accordingly. May also fire events.
				case Some(fixed) =>
					if (animationChanged || !event.oldValue.fixedState.contains(fixed)) {
						if (currentProgress == fixed)
							animatedPointer.update()
						else
							progressPointer.value = fixed
						
						// Looping single frame is considered an infinite animation
						if (handler.mayBeHandled) {
							if (instruction.loops)
								handler.onAnimationEvent(Started())
							else
								handler.onAnimationEvent(Completed(loops = false))
						}
					}
				// Case: Animating => Updates the progress and/or animation, if applicable
				case None =>
					val oldProgress = currentProgress
					// Case: Progress should be preserved, if possible
					if (newInstruction.continues) {
						// Case: Can't preserve progress => Places the progress at the target clip end point
						if (oldProgress > newInstruction.end)
							progressPointer.value = newInstruction.end
						// Case: Progress is preserved => Updates animation
						else
							animatedPointer.update()
					}
					// Case: Already at the correct progress => Updates animation
					else if (oldProgress == newInstruction.start)
						animatedPointer.update()
					// Case: Progress needs to be altered => Updates progress, which also updates the animation
					else
						progressPointer.value = newInstruction.start
					
					// Resets the finished-state
					_finishedFlag.foreach { _.leftOption.foreach { _.reset() } }
					
					// Fires animation started events, if appropriate
					if ((animationChanged || oldProgress != currentProgress) && handler.mayBeHandled)
						handler.onAnimationEvent(Started())
			}
		}
	}
	
	
	// COMPUTED -------------------------------
	
	private def instruction = instructionPointer.value
	
	/**
	  * @return Whether this animator is currently looping
	  */
	def loops = instruction.loops
	/**
	  * @return Whether this animator will certainly loop indefinitely
	  */
	def loopsForever = instructionPointer.existsFixed { _.loops }
	
	/**
	  * @return The current animation progress. Between 0 and 1.
	  */
	def currentProgress = progressPointer.value
	
	
	// IMPLEMENTED  ---------------------------
	
	override implicit def listenerLogger: Logger = instructionPointer.listenerLogger
	override protected def wrapped: Changing[A] = animatedPointer
	
	override def act(duration: FiniteDuration): Unit = {
		val i = instruction
		// Progresses the current animation
		val completionEvent = progressPointer.mutate { p =>
			val increased = p + velocityPointer.value.over(duration)
			// Case: Animation finished => Stops or loops
			if (increased >= i.end) {
				val loops = i.loops
				val stateAfter = {
					// Case: Loops => Determines the next frame
					if (loops)
						Iterator.iterate(increased) { _ - i.clip.length }.find { _ < i.end }.get
					// Case: Stops => Stops at the end
					else
						i.end
				}
				Some(Completed(loops)) -> stateAfter
			}
			else
				None -> increased
		}
		completionEvent.foreach { event =>
			// Case: Animation completes => Marks the finished flag so that action events may be ignored
			if (event.stops)
				_finishedFlag.foreach { _.either.set() }
				
			// Delivers the event, if appropriate
			if (handler.mayBeHandled)
				handler.onAnimationEvent(event)
		}
	}
}
