package utopia.genesis.test.unit

import utopia.flow.async.process.Wait
import utopia.paradigm.generic.ParadigmDataType
import utopia.flow.test.TestContext._
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.action.{ActionLoop, ActorHandler}
import utopia.genesis.handling.event.animation.{AnimationListener, Animator, AnimatorInstruction}
import utopia.paradigm.animation.Animation

/**
  * Tests the Animator class
  * @author Mikko Hilpinen
  * @since 16.08.2024, v4.0.1
  */
object AnimatorTest extends App
{
	ParadigmDataType.setup()
	
	private val targetPointer = EventfulPointer(1.0)
	
	private val instructionPointer = targetPointer.strongMap { t =>
		AnimatorInstruction(Animation.progress(0.0, t).over(1.seconds))
	}
	private val animator = new Animator(instructionPointer)
	
	private val actorHandler = ActorHandler()
	actorHandler += animator
	
	private val actionLoop = new ActionLoop(actorHandler)
	
	assert(animator.value == 0.0)
	
	private var calls = 0
	animator.addListener { _ => calls += 1 }
	
	animator.addListener { e => println(e.newValue) }
	animator.handler += AnimationListener { println(_) }
	
	println("Testing animator...")
	actionLoop.runAsync()
	
	Wait(0.3.seconds)
	
	assert(animator.value > 0.0)
	assert(animator.value < 1.0)
	assert(calls > 0)
	
	Wait(0.9.seconds)
	
	assert(animator.value == 1.0)
	assert(calls > 10)
	
	Wait(0.2.seconds)
	
	assert(animator.value == 1.0)
	
	println("\nSetting next target to 2")
	calls = 0
	targetPointer.value = 2.0
	
	assert(animator.value < 0.3)
	assert(calls < 30)
	
	Wait(0.4.seconds)
	
	assert(animator.value > 0.1, animator.value)
	assert(animator.value < 1.9)
	
	Wait(0.8.seconds)
	
	assert(animator.value == 2.0)
	assert(calls > 10)
	
	println("Success!")
}
