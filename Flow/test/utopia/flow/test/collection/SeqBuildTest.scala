package utopia.flow.test.collection

import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}

/**
  * Tests optimized seg building
  * @author Mikko Hilpinen
  * @since 12.06.2024, v2.4
  */
object SeqBuildTest extends App
{
	val b = OptimizedIndexedSeq.newBuilder[Int]
	
	assert(b.result() == Empty)
	b.clear()
	
	b += 1
	
	assert(b.result() == Single(1))
	b.clear()
	
	b += 1
	b += 2
	
	assert(b.result() == Pair(1, 2))
	b.clear()
	
	b ++= Single(1)
	
	assert(b.result() == Single(1))
	b.clear()
	
	b ++= Pair(1, 2)
	
	assert(b.result() == Pair(1, 2))
	b.clear()
	
	b ++= Vector(1, 2, 3)
	
	assert(b.result() == Vector(1, 2, 3))
	b.clear()
	
	b ++= Single(1)
	b ++= Single(2)
	
	assert(b.result() == Pair(1, 2))
	b.clear()
	
	b ++= Pair(1, 2)
	b ++= Pair(3, 4)
	
	assert(b.result() == Vector(1, 2, 3, 4))
	b.clear()
	
	b ++= Single(1)
	b += 2
	
	assert(b.result() == Pair(1, 2))
	b.clear()
	
	println("Success!")
}
