package utopia.flow.test.event

import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.Identity
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer

import scala.util.Try

/**
  * Tests Changing.incrementalFlatMap(...)
  * @author Mikko Hilpinen
  * @since 25.8.2023, v2.2
  */
object IncrementalChangingFlatMapTest extends App
{
	import utopia.flow.test.TestContext._
	
	/*
	nameInputPointer.delayedBy(inputDelay)
		.incrementalFlatMap { nameInput => optionsPointerForNameInput(api, nameInput) } {
			(previous, nameInput) => optionsPointerForNameInput(api, nameInput.newValue, Some(previous)) }
	 */
	val delay = 0.1.seconds
	val sourcePointer1 = EventfulPointer(1)
	val sourcePointer2 = EventfulPointer(1)
	
	val indirect1 = sourcePointer1.map(Identity)
	val indirect2 = sourcePointer2.map(Identity)
	val delayed1 = indirect1.delayedBy(delay)
	val delayed2 = indirect2.delayedBy(delay)
	
	def process(input: Int) = {
		delayed2.map { len => Iterator.iterate(input) { _ + 1 }.collectNext(len) }
	}
	
	var mapCalls = 1
	val mapped = delayed1.incrementalFlatMap(process) { (previous, event) =>
		mapCalls += 1
		val diff = event.newValue - event.oldValue
		// Case: Input 1 increases => Increases output by that same amount via mapping
		if (diff > 0)
			previous.map { _.map { _ + diff } }
		// Case: Input 1 decreases => Performs a new mapping operation for source pointer 2
		else
			process(event.newValue)
	}
	val indirectMapped = mapped.map(Identity)
	var lastResult = indirectMapped.value
	indirectMapped.addListener { e => lastResult = e.newValue }
	
	var lastInput1 = indirect1.value
	indirect1.addListener { e => lastInput1 = e.newValue }
	var lastInput2 = indirect2.value
	indirect2.addListener { e => lastInput2 = e.newValue }
	
	def testListenerCount(context: => String) = {
		try {
			// Expects everything to be changing
			assert(sourcePointer1.isChanging)
			assert(sourcePointer2.isChanging)
			assert(indirect1.isChanging)
			assert(indirect2.isChanging)
			assert(delayed1.isChanging)
			assert(delayed2.isChanging)
			assert(mapped.isChanging)
			// Source pointers are mapped to indirect pointers
			assert(sourcePointer1.numberOfListeners == 1)
			assert(sourcePointer2.numberOfListeners == 1)
			// Indirect pointers are mapped to delayed pointers and listeners
			assert(indirect1.numberOfListeners == 2)
			assert(indirect2.numberOfListeners == 2)
			// Delayed pointers are mapped or flat-mapped
			assert(delayed1.hasListeners)
			assert(delayed2.hasListeners)
			// Result pointer has indirect mapping
			assert(mapped.numberOfListeners == 1)
			// Indirect mapping has a listener
			assert(indirectMapped.numberOfListeners == 1)
		} catch {
			case e: Throwable =>
				println(s"\nListener counts $context:")
				println(s"\tSource 1 & 2: ${sourcePointer1.numberOfListeners} & ${sourcePointer2.numberOfListeners}")
				println(s"\tIndirect 1 & 2: ${indirect1.numberOfListeners} & ${indirect2.numberOfListeners}")
				println(s"\tDelayed 1: ${delayed1.numberOfListeners}")
				println(s"\tDelayed 2: ${delayed2.numberOfListeners}")
				println(s"\tMapped & Indirect Mapped: ${mapped.numberOfListeners} & ${indirectMapped.numberOfListeners}")
				throw e
		}
	}
	
	def test(input1: Int = sourcePointer1.value, input2: Int = sourcePointer2.value) = {
		println(s"\nStarting test ${sourcePointer1.value} & ${sourcePointer2.value} => $input1 & $input2")
		testListenerCount("before change")
		
		val mapCallsBefore = mapCalls
		val input1Before = sourcePointer1.value
		val input2Before = sourcePointer2.value
		val resultBeforeChange = Iterator.iterate(input1Before) { _ + 1 }.collectNext(input2Before)
		val expectedResult = Iterator.iterate(input1) { _ + 1 }.collectNext(input2)
		
		sourcePointer1.value = input1
		sourcePointer2.value = input2
		
		Try {
			testListenerCount("after change")
			assert(mapCalls == mapCallsBefore, "Mapping occurred too early")
			assert(indirect1.value == input1, s"Expected $input1, received ${ indirect1.value }")
			assert(indirect2.value == input2, s"Expected $input2, received ${ indirect2.value }")
			assert(delayed1.value == input1Before, s"Expected $input1Before, received ${ delayed1.value }")
			assert(delayed2.value == input2Before, s"Expected $input2Before, received ${ delayed2.value }")
			assert(mapped.value == resultBeforeChange, s"Expected $resultBeforeChange, received ${ mapped.value }")
			assert(indirectMapped.value == resultBeforeChange, s"Expected $resultBeforeChange, received ${ indirectMapped.value }")
			assert(lastResult == resultBeforeChange, s"Expected $resultBeforeChange, received $lastResult")
			
			Wait(delay * 2)
			
			testListenerCount("after delay")
			if (input1 == input1Before)
				assert(mapCalls == mapCallsBefore, "Mapping occurred unnecessarily")
			else
				assert(mapCalls == mapCallsBefore + 1, "No mapping occurred")
			assert(delayed1.value == input1, s"Expected $input1, received ${ delayed1.value }")
			assert(delayed2.value == input2, s"Expected $input2, received ${ delayed2.value }")
			
			assert(mapped.value == expectedResult, s"Expected $expectedResult, received ${ mapped.value }")
			assert(indirectMapped.value == expectedResult, s"Expected $expectedResult, received ${ indirectMapped.value }")
			assert(lastResult == expectedResult, s"Expected $expectedResult, received $lastResult")
		}.failure
			.foreach { error =>
				println(s"\nInputs: $input1Before & $input2Before => $input1 & $input2")
				println(s"Expected result: $resultBeforeChange => $expectedResult")
				println(s"1: Indirect & Delayed & Listened = ${indirect1.value} & ${delayed1.value} & $lastInput1")
				println(s"2: Indirect & Delayed & Listened = ${indirect2.value} & ${delayed2.value} & $lastInput2")
				println(s"Result: Direct & Indirect & Listened = ${mapped.value} & ${indirectMapped.value} & $lastResult")
				println(s"Delay 1 listeners: ${delayed1.numberOfListeners}")
				println(s"Delay 2 listeners: ${delayed2.numberOfListeners}")
				
				throw error
			}
	}
	
	test()
	test(input2 = 2)
	test(input1 = 2)
	test(input2 = 3)
	test(input1 = 3)
	test(input1 = 2)
	test(input2 = 1)
	test(input2 = 2)
	
	println("Success!")
}
