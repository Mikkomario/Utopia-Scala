package utopia.flow.test.collection

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.mutable.iterator.{PollableOnce, RepeatOneForeverIterator}

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
	assert(iter1.collectWhile { _ <= 10 }.size == 8)
	assert(iter1.next() == 11)
	assert(iter1.take(10).size == 10)
	assert(iter1.poll == 22)
	iter1.collectWhile { _ < 100 }
	assert(iter1.poll == 100)
	iter1.next()
	assert(!iter1.hasNext)
	assert(iter1.pollOption.isEmpty)
	assert(iter1.nextOption().isEmpty)
	
	val once1 = PollableOnce(5)
	assert(once1.hasNext)
	assert(once1.next() == 5)
	assert(!once1.hasNext)
	
	val once2 = PollableOnce(5).map { _ + 1 }
	assert(once2.hasNext)
	assert(once2.next() == 6)
	assert(!once2.hasNext)
	
	val iter2 = (1 to 100).iterator.pollable
	assert(iter2.next() == 1)
	assert(iter2.poll == 2)
	val mappedIter2 = iter2.map { -_ }
	assert(mappedIter2.next() == -2)
	assert(mappedIter2.next() == -3)
	
	val iter3 = (1 to 100).iterator.pollable
	assert(iter3.next() == 1)
	assert(iter3.poll == 2)
	val mappedIter3 = iter3.map { -_ }.pollable
	assert(mappedIter3.poll == -2)
	assert(mappedIter3.next() == -2)
	assert(mappedIter3.next() == -3)
	
	assert((1 to 3).iterator.pairedFrom(0).toVector == Vector(Pair(0, 1), Pair(1, 2), Pair(2, 3)))
	
	var foreverValueCalls = 0
	val forever = RepeatOneForeverIterator {
		foreverValueCalls += 1
		1
	}
	assert(forever.hasNext)
	assert(foreverValueCalls == 0)
	assert(forever.next() == 1)
	assert(foreverValueCalls == 1)
	assert(forever.next() == 1)
	assert(foreverValueCalls == 1)
	
	println("Success!")
}
