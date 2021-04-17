package utopia.flow.test.collection

import utopia.flow.util.CollectionExtensions._

/**
 * Tests iterator -related functions
 * @author Mikko Hilpinen
 * @since 5.4.2021, v1.9
 */
object IteratorTest extends App
{
	// Iterator from 1 to 100
	val iter1 = (1 to 100).iterator.pollable
	
	assert(iter1.poll == 1)
	assert(iter1.poll == 1)
	assert(iter1.next() == 1)
	assert(iter1.next() == 2)
	assert(iter1.poll == 3)
	assert(iter1.takeNextWhile { _ <= 10 }.size == 8)
	assert(iter1.next() == 11)
	assert(iter1.takeNext(10).size == 10)
	assert(iter1.poll == 22)
	iter1.takeNextWhile { _ < 100 }
	assert(iter1.poll == 100)
	iter1.next()
	assert(!iter1.hasNext)
	assert(iter1.pollOption.isEmpty)
	assert(iter1.nextOption().isEmpty)
	
	println("Success!")
}
