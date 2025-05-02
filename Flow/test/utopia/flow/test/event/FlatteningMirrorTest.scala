package utopia.flow.test.event

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.test.TestContext._
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.ResettableFlag

/**
  * Tests flattening of changing items
  * @author Mikko Hilpinen
  * @since 01.05.2025, v2.6
  */
object FlatteningMirrorTest extends App
{
	// Sets up the pointers
	// Uses conditional flat-map
	// Does not apply continuous listeners, yet
	
	private val origin = Pointer.lockable(1)
	private val positiveStepsP = Pointer.lockable(1)
	private val negativeStepsP = Pointer.eventful(0)
	private val condition = ResettableFlag(initialValue = true)
	private var mapCalls = 0
	private val mirror = origin.flatMapWhile(condition) { i =>
		mapCalls += 1
		if (i >= 0) positiveStepsP.map { i + _ } else negativeStepsP.map { i - _ }
	}
	
	assert(positiveStepsP.hasNoListeners)
	assert(negativeStepsP.hasNoListeners)
	assert(mirror.value == 2)
	assert(mapCalls == 1)
	assert(negativeStepsP.hasNoListeners)
	
	// Adjusts + steps => Expects the mirror to update
	positiveStepsP.value = 2
	
	assert(mapCalls == 1)
	assert(mirror.value == 3)
	assert(mapCalls == 1)
	
	// Adjusts - steps => Expects the mirror to remain unchanged
	negativeStepsP.value = 1
	
	assert(mapCalls == 1)
	assert(mirror.value == 3)
	assert(mapCalls == 1)
	
	// Adjusts the origin value, keeping it + => Expects flat-mapping to occur lazily
	origin.value = 2
	
	assert(mapCalls == 1)
	assert(mirror.value == 4)
	assert(mapCalls == 2)
	
	// Changes the origin value to - => Expects lazy flat-map and changed value
	origin.value = -1
	
	assert(mapCalls == 2)
	assert(mirror.value == -2)
	assert(mapCalls == 3)
	
	// Adjusts + steps => Expects the mirror to remain unchanged
	positiveStepsP.value = 1
	
	assert(mapCalls == 3)
	assert(mirror.value == -2)
	assert(mapCalls == 3)
	
	// Adjusts the origin twice => Expects only the latter to count
	origin.value = 1
	origin.value = -2
	
	assert(mapCalls == 3)
	assert(mirror.value == -3)
	assert(mapCalls == 4)
	
	// Next, starts active tracking -tests
	
	private var lastValue = 0
	private val lastValueUpdator = ChangeListener[Int] { e => lastValue = e.newValue }
	mirror.addListener(lastValueUpdator)
	
	assert(lastValue == 0)
	
	// Changes the steps value twice => Expects it to count both times
	negativeStepsP.value = 2
	
	assert(lastValue == -4)
	
	negativeStepsP.value = 3
	
	assert(lastValue == -5)
	assert(mirror.value == -5)
	
	// Changes the + steps => Expects it to have no effect
	positiveStepsP.value = 1
	
	assert(mapCalls == 4)
	assert(lastValue == -5)
	assert(mirror.value == -5)
	
	// Changes the origin twice => Expects it to count both times
	origin.value = 1
	
	assert(mapCalls == 5)
	assert(lastValue == 2)
	
	origin.value = 2
	
	assert(mapCalls == 6)
	assert(lastValue == 3)
	assert(mirror.value == 3)
	
	// Changes - steps => Expects it to have no effect
	negativeStepsP.value = 1
	
	assert(lastValue == 3)
	assert(mirror.value == 3)
	
	// Changes + steps twice => Expects it to have effect both times
	positiveStepsP.value = 2
	
	assert(lastValue == 4)
	
	positiveStepsP.value = 3
	
	assert(lastValue == 5)
	assert(mirror.value == 5)
	
	// Stops active listening and then modifies the step => Expects no more events to be fired
	mirror.removeListener(lastValueUpdator)
	positiveStepsP.value = 2
	
	assert(lastValue == 5)
	
	positiveStepsP.value = 1
	
	assert(lastValue == 5)
	assert(mirror.value == 3)
	
	// Modifies the origin twice => Expects the effects to be lazy again
	origin.value = -2
	
	assert(mapCalls == 6)
	assert(lastValue == 5)
	
	origin.value = -1
	
	assert(mapCalls == 6)
	assert(lastValue == 5)
	assert(mirror.value == -2)
	assert(mapCalls == 7)
	assert(lastValue == 5)
	
	// Re-attaches the listener and changes the step value twice => Expects an immediate change both times
	mirror.addListener(lastValueUpdator)
	negativeStepsP.value = 2
	
	assert(lastValue == -3)
	
	negativeStepsP.value = 1
	
	assert(lastValue == -2)
	assert(mirror.value == -2)
	
	// Sets back to positive for the next tests
	assert(mirror.destiny == MaySeal)
	
	origin.value = 1
	
	assert(mapCalls == 8, mapCalls)
	assert(lastValue == 2)
	
	// Tests locking behavior
	
	assert(mirror.destiny == MaySeal)
	
	// Locks the origin pointer => Expects the mirror to remain changing & responsive
	origin.lock()
	
	assert(origin.destiny == Sealed)
	assert(mirror.destiny == MaySeal)
	assert(mapCalls == 8, mapCalls)
	assert(lastValue == 2)
	
	positiveStepsP.value = 2
	
	assert(lastValue == 3)
	assert(mirror.value == 3)
	
	// Locks the steps pointer => Expects the mirror to be fixed
	private var stopEventReceived = false
	mirror.addChangingStoppedListenerAndSimulateEvent { stopEventReceived = true }
	
	assert(!stopEventReceived)
	
	positiveStepsP.lock()
	
	assert(stopEventReceived)
	assert(lastValue == 3)
	assert(mirror.destiny == Sealed)
	assert(mirror.value == 3)
	
	println("Done!")
}
