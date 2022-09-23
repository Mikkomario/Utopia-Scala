package utopia.flow.test.collection

import utopia.flow.collection.immutable.caching.lazily.Lazy
import utopia.flow.util.CollectionExtensions._

/**
  * Tests lazy collection implementations
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
object LazyCollectionsTest extends App
{
	private val iter1 = new CountingIterator(Iterator.iterate(0) { _ + 1 }.take(10))
	val c1 = iter1.caching
	val c13 = c1.take(3)
	
	assert(iter1.calls == 0, "unnecessary calls")
	assert(c1.head == 0)
	assert(iter1.calls == 1)
	assert(c13.size == 3, "take size doesn't match")
	assert(iter1.calls == 3)
	assert(c13.size == 3)
	assert(iter1.calls == 3)
	assert(c1.head == 0)
	assert(iter1.calls == 3)
	
	private val iter2 = new CountingIterator(Iterator.iterate(0) { _ + 1 }.take(10))
	var mapCalls = 0
	val c2 = iter2.lazyMap { n => mapCalls += 1; n + 10 }
	
	assert(iter2.calls == 0)
	assert(mapCalls == 0)
	assert(c2.head == 10)
	assert(iter2.calls == 1)
	assert(mapCalls == 1)
	assert(c2.size == 10)
	assert(iter2.calls == 10)
	assert(mapCalls == 1)
	assert(c2.last == 19)
	assert(mapCalls == 2)
	
	val c2plus = Lazy { mapCalls += 1; -10 } +: c2
	
	assert(mapCalls == 2)
	assert(c2plus.head == -10)
	assert(mapCalls == 3)
	assert(c2plus.head == -10)
	assert(mapCalls == 3)
	
	private val iter3 = new CountingIterator(Iterator.iterate(0) { _ + 1 }.take(10))
	var mapCalls3 = 0
	val c3 = iter3.lazyMap { n => mapCalls3 += 1; n + 10 }.toLazyVector
	
	assert(iter3.calls == 10)
	assert(mapCalls3 == 0)
	assert(c3(2) == 12, c3(2))
	assert(mapCalls3 == 1)
	assert(c3(2) == 12)
	assert(mapCalls3 == 1)
	assert(c3.take(3).size == 3)
	assert(mapCalls3 == 1)
	assert(c3.take(3).sum == 33)
	assert(mapCalls3 == 3)
	
	println("Success!")
	
	private class CountingIterator[A](source: Iterator[A]) extends Iterator[A] {
		var calls = 0
		
		override def hasNext = source.hasNext
		
		override def next() = {
			calls += 1
			source.next()
		}
	}
}
