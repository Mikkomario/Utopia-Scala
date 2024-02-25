package utopia.flow.test.collection

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.Pair.PairOrVectorBuilder

/**
  * Tests certain Pair functions
  * @author Mikko Hilpinen
  * @since 25/02/2024, v2.4
  */
object PairTest extends App
{
	val p1 = Pair(1, 2)
	
	assert((p1 ++ Vector.empty).isInstanceOf[Pair[Int]], p1 ++ Vector.empty)
	
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
	
	println("Success!")
}
