package utopia.flow.test.event

import utopia.flow.view.mutable.eventful.EventfulPointer

/**
  * Tests various pointers
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  */
object ChangingTest extends App
{
	// Tests mapping
	val origin = new EventfulPointer(1)
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
	assert(lightMergeMapped.value == -2)
	
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
	
	println("Done!")
}
