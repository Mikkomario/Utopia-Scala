package utopia.flow.test.collection

import utopia.flow.collection.CollectionExtensions._

/**
  * Tests certain CachingSeq functions
  * @author Mikko Hilpinen
  * @since 13.06.2024, v2.4
  */
object CachingSeqTest extends App
{
	var mapCalls = 0
	val s = Vector(1, 2, 3, 4).mapCaching { i =>
		mapCalls += 1
		i + 1
	}
	
	assert(mapCalls == 0)
	assert(s.knownSize == 4)
	assert(s.size == 4)
	assert(mapCalls == 0)
	
	assert(s.head == 2)
	assert(mapCalls == 1, mapCalls)
	
	val s2 = s.take(2)
	
	assert(mapCalls == 1)
	assert(s2.head == 2)
	
	assert(s2.last == 3)
	assert(mapCalls == 2)
	assert(s.currentSize == 2)
	
	assert(!s.isDefinedAt(4))
	assert(mapCalls == 2)
	
	assert(s(1) == 3, s(1))
	val a = s(2)
	assert(a == 4, a)
	assert(mapCalls == 3)
	
	val s3 = s.drop(3)
	
	assert(mapCalls == 3)
	assert(s3.currentSize == 0)
	
	assert(s3.head == 5)
	assert(mapCalls == 4)
	assert(s.currentSize == 4)
	
	println("Success!")
}
