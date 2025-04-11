package utopia.flow.test.collection

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.LazyPair
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.operator.Identity
import utopia.flow.operator.enumeration.End

/**
  * Tests certain Pair functions
  * @author Mikko Hilpinen
  * @since 25/02/2024, v2.4
  */
object PairTest extends App
{
	private val p1 = Pair(1, 2)
	
	// Tests iteration
	private val iter = p1.iterator
	
	assert(iter.hasNext)
	assert(iter.next() == 1)
	assert(iter.hasNext)
	assert(iter.next() == 2)
	assert(!iter.hasNext)
	
	// Tests ++
	assert((p1 ++ Vector.empty).isInstanceOf[Pair[Int]], p1 ++ Vector.empty)
	
	// Tests filter
	assert(p1.filter { _ > 0 } == p1)
	assert(p1.filter { _ < 0 } == Vector.empty)
	
	// Tests flatMap
	assert(p1.flatMap { i => Vector.fill(i - 1)(i) } == Single(2))
	
	// Tests lazy pair
	private val counter = Iterator.iterate(1) { _ + 1 }.pollable
	private val p2 = LazyPair.fill { counter.next() }
	
	assert(p2.current.isEmpty)
	assert(p2.second == 1)
	assert(counter.poll == 2)
	assert(p2.current.size == 1)
	assert(p2.first == 2)
	
	println("---")
	End.values.foreach(println)
	println("---")
	End.values.flatMap { e => println(e); None }
	
	// Tests sort
	assert(p1.sorted == p1)
	assert(p1.reverseSorted == Pair(2, 1))
	assert(p1.sortBy(Identity) == p1)
	assert(p1.sortBy { -_ } == Pair(2, 1))
	
	println("Success!")
}
