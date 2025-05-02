package utopia.flow.test.event

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.test.TestContext._
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.Flag

/**
  * Tests Changing.mapWhile(...)
  * @author Mikko Hilpinen
  * @since 02.05.2025, v2.6
  */
object ConditionalMirroringTest extends App
{
	// Sets up the test pointers
	
	private val origin = Pointer.eventful(1)
	private val condition = Flag.resettable.lockable(initialState = true)
	private val mirror = origin.mapWhile(condition) { i =>
		println(s"Mapping $i")
		i + 1
	}
	
	// Performs tests without active listeners
	
	// Performs basic tests during active mapping
	assert(mirror.value == 2)
	
	origin.value = 2
	
	assert(mirror.value == 3)
	
	// Disables mapping condition and tests whether mirroring is disabled
	condition.reset()
	origin.value = 1
	
	assert(mirror.value == 3)
	
	origin.value = 0
	
	assert(mirror.value == 3)
	
	// Re-enables mapping => Expects an immediate effect and responsiveness to future changes
	condition.set()
	
	assert(mirror.value == 1)
	
	origin.value = 1
	
	assert(mirror.value == 2)
	
	// Next tests are with active listening
	private var lastValue = 0
	private val updateListener = ChangeListener[Int] { e => lastValue = e.newValue }
	mirror.addListener(updateListener)
	
	assert(lastValue == 0)
	assert(mirror.value == 2)
	
	// Makes sure events are fired immediately
	origin.value = 2
	
	assert(lastValue == 3)
	assert(mirror.value == 3)
	
	// Makes sure events are fired without call to .value
	origin.value = 3
	
	assert(lastValue == 4)
	
	origin.value = 4
	
	assert(lastValue == 5)
	assert(mirror.value == 5)
	
	// Disables mirroring => Expects no further responses to value changes
	println("Disables mirroring")
	condition.reset()
	origin.value = 0
	
	assert(lastValue == 5, lastValue)
	assert(mirror.value == 5)
	
	origin.value = 1
	
	assert(lastValue == 5)
	
	origin.value = 2
	
	assert(lastValue == 5)
	assert(mirror.value == 5)
	
	// Re-enables mirroring => Expects immediate effect and responsiveness to further changes
	println("Re-enables mirroring")
	condition.set()
	
	assert(lastValue == 3)
	assert(mirror.value == 3)
	
	origin.value = 1
	
	assert(lastValue == 2)
	assert(mirror.value == 2)
	
	// Tests condition locking (to a false position)
	
	private var stopEventReceived = false
	mirror.addChangingStoppedListenerAndSimulateEvent { stopEventReceived = true }
	
	assert(!stopEventReceived)
	assert(mirror.destiny == MaySeal, mirror.destiny)
	
	// Disables mapping and mutates the value => Expects the mirrored value to remain, as previously
	condition.reset()
	origin.value = 0
	
	assert(lastValue == 2)
	assert(mirror.value == 2)
	assert(mirror.destiny == MaySeal)
	
	// Locks the condition => Expects the mirror to stop changing and stop tracking the origin pointer
	condition.lock()
	
	assert(stopEventReceived)
	assert(lastValue == 2)
	assert(mirror.destiny == Sealed)
	assert(mirror.value == 2)
	assert(origin.hasNoListeners)
	
	println("Success!")
}
