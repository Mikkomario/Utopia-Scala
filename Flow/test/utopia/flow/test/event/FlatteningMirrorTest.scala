package utopia.flow.test.event

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.operator.Identity
import utopia.flow.test.TestContext._
import utopia.flow.view.immutable.eventful.AlwaysFalse
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.async.Volatile
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
		println(s"Mapping $i")
		mapCalls += 1
		if (i >= 0) {
			println(s"Mid-pointer = $i + Positive steps")
			positiveStepsP.map { steps =>
				println(s"+Steps=$steps => Result=${ i + steps }")
				i + steps
			}
		}
		else {
			println(s"Mid-pointer = $i - Negative steps")
			negativeStepsP.map { steps =>
				println(s"-Steps=$steps => Result=${ i - steps }")
				i - steps
			}
		}
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
	println("\nChanges positive steps twice")
	positiveStepsP.value = 2
	
	assert(lastValue == 4)
	
	positiveStepsP.value = 3
	
	assert(lastValue == 5)
	assert(mirror.value == 5)
	
	// Stops active listening and then modifies the step => Expects no more events to be fired
	println("\nStops active listening")
	mirror.removeListener(lastValueUpdator)
	positiveStepsP.value = 2
	
	assert(lastValue == 5)
	
	println("\nSets +Steps=1")
	positiveStepsP.value = 1
	
	assert(lastValue == 5)
	assert(mirror.value == 3, mirror.value) // FIXME: Fails here (mid-pointer's value doesn't update)
	
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
	
	// Makes sure the mapping condition is respected
	condition.reset()
	origin.value = 1
	
	assert(mapCalls == 7, mapCalls)
	assert(lastValue == -2)
	assert(mirror.value == -2)
	
	negativeStepsP.value = 2
	
	assert(lastValue == -3)
	assert(mirror.value == -3)
	
	positiveStepsP.value = 2
	
	assert(lastValue == -3)
	assert(mirror.value == -3)
	assert(mirror.destiny == MaySeal)
	
	// Reactivates mapping
	condition.set()
	
	assert(mapCalls == 8)
	assert(lastValue == 3)
	assert(mirror.value == 3)
	
	// Tests locking behavior
	
	assert(mirror.destiny == MaySeal)
	
	// Locks the origin pointer => Expects the mirror to remain changing & responsive
	origin.lock()
	
	assert(origin.destiny == Sealed)
	assert(mirror.destiny == MaySeal)
	assert(mapCalls == 8, mapCalls)
	assert(lastValue == 3)
	
	positiveStepsP.value = 1
	
	assert(lastValue == 2)
	assert(mirror.value == 2)
	
	// Locks the steps pointer => Expects the mirror to be fixed
	private var stopEventReceived = false
	mirror.addChangingStoppedListenerAndSimulateEvent { stopEventReceived = true }
	
	assert(!stopEventReceived)
	
	positiveStepsP.lock()
	
	assert(stopEventReceived)
	assert(lastValue == 2)
	assert(mirror.destiny == Sealed)
	assert(mirror.value == 2)
	
	// Tests pop-up visible -type setup
	println("\n\nStarts flag test")
	private var flagChanges = 0
	private val flagP = Volatile.lockable.empty[ResettableFlag]
	private val flattenedFlag = flagP.flatMap { flag =>
		flagChanges += 1
		flag.getOrElse(AlwaysFalse)
	}
	println("Starts listening to the flattened flag")
	private var lastFlagValue = false
	flattenedFlag.addListener { e => lastFlagValue = e.newValue }
	
	println("Specifies the flag")
	private val flag1 = ResettableFlag()
	flagP.setOne(flag1)
	assert(!lastFlagValue)
	
	println("Sets the flag")
	flag1.set()
	assert(lastFlagValue)
	
	flag1.reset()
	assert(!lastFlagValue)
	
	private val flag2 = ResettableFlag(initialValue = true)
	flagP.setOne(flag2)
	assert(lastFlagValue)
	
	println("Success!")
}
