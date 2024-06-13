package utopia.flow.test.collection

import utopia.flow.collection.immutable.{Pair, Single}

/**
  * Tests Single
  * @author Mikko Hilpinen
  * @since 12.06.2024, v2.4
  */
object SingleTest extends App
{
	val s = Single(1)
	
	assert(s.nonEmpty)
	assert(!s.isEmpty)
	assert(s.size == 1)
	
	assert(s.head == 1)
	assert(s.last == 1)
	assert(s.headOption.contains(1))
	assert(s.lastOption.contains(1))
	
	assert(s :+ 2 == Pair(1, 2))
	assert(2 +: s == Pair(2, 1))
	
	assert(s ++ Vector(2, 3) == Vector(1, 2, 3))
	assert(Vector(2, 3) ++ s == Vector(2, 3, 1))
	
	var v = 0
	s.foreach { v += _ }
	assert(v == 1)
	
	v = 0
	s.iterator.foreach { v += _ }
	assert(v == 1)
	
	println("Done!")
}
