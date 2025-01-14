package utopia.flow.test.event

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.event.model.{ChangeEvent, ChangeResult}
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType.{DoubleType, StringType}
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.test.TestContext._
import utopia.flow.util.EitherExtensions._
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.async.{EventfulVolatile, LockableVolatile}
import utopia.flow.view.mutable.eventful.{EventfulPointer, LockablePointer, ResettableFlag, SettableFlag}

import scala.util.Try

/**
  * Tests various pointers
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  */
object ChangingTest extends App
{
	// Tests advanced change event functions
	val ce = ChangeEvent[IndexedSeq[Int]](Pair(1, 2), Vector(2, 3, 4))
	
	assert(ce.added == Pair(3,4))
	assert(ce.removed == Single(1))
	
	// Tests listening
	def testListening(p: EventfulPointer[Int]) = {
		var lastValue = -1
		
		p.addListenerAndSimulateEvent(-1) { e => lastValue = e.newValue }
		assert(lastValue == 1)
		
		p.value = 55
		assert(lastValue == 55)
		
		p.value = -2
		assert(lastValue == -2)
	}
	testListening(EventfulPointer(1))
	testListening(EventfulVolatile(1))
	testListening(LockablePointer(1))
	testListening(LockableVolatile(1))
	
	// Tests mapping
	val origin = EventfulPointer(1)
	val mapped = origin.map { _ % 2 }
	
	var originChanges = 0
	var mappedChanges = 0
	origin.addListener { _ => originChanges += 1 }
	mapped.addListener { _ => mappedChanges += 1 }
	
	assert(origin.value == 1)
	assert(mapped.value == 1)
	assert(originChanges == 0)
	assert(mappedChanges == 0)
	
	origin.value = 2
	
	assert(origin.value == 2)
	assert(mapped.value == 0)
	assert(originChanges == 1)
	assert(mappedChanges == 1)
	
	origin.value = 4
	
	assert(origin.value == 4)
	assert(mapped.value == 0)
	assert(originChanges == 2)
	assert(mappedChanges == 1)
	
	// Tests light merge listening
	println("\nTesting light merge listening")
	val o5 = EventfulPointer(1)
	val o6 = EventfulPointer(2)
	val lm2 = o5.lightMergeWith(o6) { _ + _ }
	var lastLm2EventValue = -1
	
	lm2.addChangingStoppedListenerAndSimulateEvent { throw new IllegalStateException("'lm2' stopped changing") }
	println("Adding listener to lm2")
	lm2.addListenerAndSimulateEvent(-1) { e => lastLm2EventValue = e.newValue }
	
	println(lm2)
	assert(lm2.hasListeners)
	assert(lastLm2EventValue == 3)
	
	o5.value = 4
	println(lm2)
	assert(lm2.hasListeners)
	assert(lastLm2EventValue == 6, lastLm2EventValue)
	
	o6.value = -2
	assert(lm2.hasListeners)
	assert(lastLm2EventValue == 2, lastLm2EventValue)
	assert(lm2.value == 2)
	
	// Tests merging
	val o2 = EventfulPointer(1)
	val o3 = EventfulPointer(2)
	val merged = o2.mergeWith(o3) { _ + _ }
	val lightMerged = o2.lightMergeWith(o3) { _ + _ }
	
	assert(o2.value == 1)
	assert(o3.value == 2)
	assert(merged.value == 3)
	assert(lightMerged.value == 3)
	
	o2.value = 5
	
	val mergeMapped = merged.strongMap { -_ }
	val lightMergeMapped = lightMerged.map { -_ }
	
	assert(merged.value == 7)
	assert(lightMerged.value == 7)
	assert(mergeMapped.value == -7)
	assert(lightMergeMapped.value == -7)
	
	o3.value = -3
	
	assert(merged.value == 2)
	assert(lightMerged.value == 2)
	assert(mergeMapped.value == -2)
	assert(o2.mayChange)
	assert(o3.mayChange)
	assert(merged.mayChange)
	assert(lightMerged.mayChange)
	assert(lightMergeMapped.mayChange)
	assert(lightMergeMapped.value == -2, lightMergeMapped.value)
	
	// Tests merging edge case (event-firing in option + tuple types)
	val o4 = EventfulPointer(None)
	val merged2 = o2.mergeWith(o4) { (a, b) => Some(a -> b) }
	var listenCounter = 0
	merged2.addListener { _ => listenCounter += 1 }
	
	assert(listenCounter == 0)
	
	o2.value = 1
	o2.value = 3
	
	assert(listenCounter == 2)
	
	// Testing merging with optimized mirrors
	origin.value = 1
	o2.value = 1
	
	assert(mapped.value == 1)
	
	val merged3 = mapped.mergeWith(o2) { _ + _ }
	var merged3Changed = 0
	merged3.addListener { _ => merged3Changed += 1 }
	
	assert(merged3.value == 2)
	assert(merged3Changed == 0)
	
	origin.value = 2
	origin.value = 1
	origin.value = 2
	
	assert(merged3Changed == 3, merged3Changed)
	assert(merged3.value == 1)
	
	// Tests light merging
	val lmo1 = EventfulPointer(1)
	val lmo2 = EventfulPointer(2)
	val lm = lmo1.lightMergeWith(lmo2) { _ + _ }
	
	assert(lmo1.hasNoListeners)
	assert(lmo2.hasNoListeners)
	assert(lm.value == 3)
	assert(lmo1.hasNoListeners)
	assert(lmo2.hasNoListeners)
	
	lmo1.value = 3
	
	assert(lm.value == 5)
	
	val lmm = lm.strongMap { -_ }
	
	assert(lmo1.numberOfListeners == 1)
	assert(lmo2.numberOfListeners == 1)
	assert(lm.numberOfListeners == 1)
	assert(lmm.value == -5)
	
	// Tests pointer locking / lockable pointer
	def testLockable(lp: LockablePointer[Int]) = {
		// Also tests withState mapping
		val lps = lp.withState
		var locked = false
		var lastValue = 0
		var lastStateValue = ChangeResult.temporal(0)
		lp.onceChangingStops { locked = true }
		lp.addListener { e =>
			println(s"Updating 'lastValue' with $e")
			lastValue = e.newValue
		}
		lps.addListener { e => lastStateValue = e.newValue }
		
		assert(lp.value == 0)
		assert(lp.mayChange)
		assert(lastValue == 0)
		assert(lastStateValue == ChangeResult.temporal(0))
		assert(lps.value == ChangeResult.temporal(0))
		assert(!locked)
		
		println("Changes value from 0 to 1")
		lp.value = 1
		
		assert(lp.value == 1)
		assert(lp.mayChange)
		assert(lastValue == 1, lastValue)
		assert(lastStateValue == ChangeResult.temporal(1), lastStateValue)
		assert(lps.value == ChangeResult.temporal(1))
		
		println("Locks the pointer")
		lp.lock()
		
		assert(locked)
		assert(lp.isFixed)
		assert(lp.value == 1)
		assert(lastValue == 1)
		assert(lastStateValue == ChangeResult.finalValue(1))
		assert(lps.value == ChangeResult.finalValue(1))
		
		assert(Try { lp.value = 2 }.isFailure)
		assert(!lp.trySet(2))
		assert(lp.value == 1)
		assert(lastValue == 1)
	}
	testLockable(LockablePointer(0))
	testLockable(LockableVolatile(0))
	
	// Tests merging with 4 pointers, including an active-condition
	private val lmi1 = Pointer.eventful(1)
	private val lmi2 = Pointer.eventful(2)
	private val lmi3 = Pointer.eventful(3)
	private val lmi4 = Pointer.eventful(4)
	private val merge4ActiveFlag = ResettableFlag(initialValue = true)
	private val merge4Lock = SettableFlag()
	private val otherLmis = Vector(lmi2, lmi3, lmi4)
	private var merge4Calls = 0
	private val merge4 = lmi1.mergeWithWhile(Vector(lmi2, lmi3, lmi4), merge4ActiveFlag && !merge4Lock) { v1 =>
		merge4Calls += 1
		v1 + otherLmis.view.map { _.value }.sum
	}
	
	assert(merge4Calls == 1, merge4Calls)
	assert(merge4.destiny.isPossibleToSeal)
	assert(merge4.value == 10)
	assert(merge4Calls == 1, merge4Calls)
	
	lmi1.value = 10
	lmi2.value = -2
	
	assert(merge4Calls == 1)
	assert(merge4.value == 15)
	assert(merge4Calls == 2)
	
	merge4ActiveFlag.reset()
	lmi1.value = 1
	
	assert(merge4Calls == 2)
	assert(merge4.value == 15)
	assert(merge4Calls == 2)
	
	merge4ActiveFlag.set()
	
	assert(merge4Calls == 2)
	assert(merge4.value == 6)
	assert(merge4Calls == 3)
	
	lmi3.value = 10
	lmi4.value = -4
	merge4ActiveFlag.reset()
	
	assert(merge4Calls == 4)
	assert(merge4.value == 5)
	assert(merge4Calls == 4)
	
	lmi3.value = 3
	lmi4.value = 4
	lmi2.value = 2
	lmi1.value = 1
	
	assert(merge4Calls == 4)
	assert(merge4.value == 5)
	
	private var lastMerge4Value = -1
	merge4.addContinuousListener { e => lastMerge4Value = e.newValue }
	
	assert(merge4.hasListeners)
	assert(lastMerge4Value == -1)
	assert(merge4Calls == 4)
	assert(merge4.value == 5)
	
	merge4ActiveFlag.set()
	
	assert(merge4Calls == 5, merge4Calls)
	assert(lastMerge4Value == 10)
	assert(merge4.value == 10)
	
	lmi1.value = 10
	lmi2.value = -2
	
	assert(merge4Calls == 7)
	assert(lastMerge4Value == 15)
	assert(merge4.value == 15)
	assert(merge4Calls == 7)
	
	private var merge4Stopped = false
	merge4.onceChangingStops { merge4Stopped = true }
	
	assert(!merge4Stopped)
	
	merge4Lock.set()
	
	assert(merge4Stopped)
	assert(merge4.isFixed)
	
	lmi1.value = 1
	lmi2.value = 2
	
	assert(merge4Calls == 7)
	assert(lastMerge4Value == 15)
	assert(merge4.value == 15, merge4.value)
	
	// Tests pointer-division
	private val dp = LockablePointer(1: Value)
	private val divided = dp.divide(0.0, "") { v =>
		v.castTo(DoubleType, StringType).mapBoth { _.getDouble } { _.getString }
	}
	
	assert(divided.left.value == 1.0)
	assert(divided.right.value == "")
	assert(divided.lastUpdatedSide == First)
	assert(divided.lastUpdated == Left(divided.left))
	
	assert(divided.left.destiny == MaySeal)
	assert(divided.right.destiny == MaySeal)
	assert(divided.lastUpdatedPointerPointer.destiny == MaySeal)
	
	dp.value = "test"
	
	assert(divided.left.value == 1.0)
	assert(divided.right.value == "test")
	assert(divided.lastUpdatedSide == Last)
	assert(divided.lastUpdated == Right(divided.right))
	
	dp.value = 3.0
	
	assert(divided.left.value == 3.0)
	assert(divided.right.value == "test")
	assert(divided.lastUpdatedSide == First)
	assert(divided.lastUpdated == Left(divided.left))
	
	dp.lock()
	
	assert(divided.left.destiny == Sealed)
	assert(divided.right.destiny == Sealed)
	assert(divided.lastUpdatedSidePointer.destiny == Sealed)
	assert(divided.lastUpdatedPointerPointer.destiny == Sealed)
	
	println("Done!")
}
