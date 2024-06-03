package utopia.flow.test.collection

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.Pair.PairOrVectorBuilder
import utopia.flow.collection.immutable.caching.iterable.LazyPair

/**
  * Tests certain Pair functions
  * @author Mikko Hilpinen
  * @since 25/02/2024, v2.4
  */
object PairTest extends App
{
	// Tests ++
	val p1 = Pair(1, 2)
	
	assert((p1 ++ Vector.empty).isInstanceOf[Pair[Int]], p1 ++ Vector.empty)
	
	// Tests building
	val b1 = new PairOrVectorBuilder[Int]()
	assert(b1.result() == Left(Vector.empty))
	b1 += 1
	assert(b1.result() == Left(Vector(1)))
	b1 += 2
	assert(b1.result() == Right(p1))
	b1 += 3
	assert(b1.result() == Left(Vector(1, 2, 3)))
	b1 += 4
	assert(b1.result() == Left(Vector(1, 2, 3, 4)))
	
	// Tests filter
	assert(p1.filter { _ > 0 } == p1)
	assert(p1.filter { _ < 0 } == Vector.empty)
	
	// Tests lazy pair
	val counter = Iterator.iterate(1) { _ + 1 }.pollable
	val p2 = LazyPair.fill { counter.next() }
	
	assert(p2.current.isEmpty)
	assert(p2.second == 1)
	assert(counter.poll == 2)
	assert(p2.current.size == 1)
	assert(p2.first == 2)
	
	println("Success!")
}
