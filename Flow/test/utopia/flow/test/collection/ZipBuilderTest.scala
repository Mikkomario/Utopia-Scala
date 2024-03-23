package utopia.flow.test.collection

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.mutable.builder.ZipBuilder

/**
 * Tests the ZipBuilder class
 *
 * @author Mikko Hilpinen
 * @since 23/03/2024, v2.4
 */
object ZipBuilderTest extends App
{
	// Standard += test
	
	val b1 = ZipBuilder.pair[Int]()
	
	b1.left += 1
	b1.left += 2
	b1.left += 3
	b1.right += 1
	b1.right += 2
	b1.right += 3
	
	val res1 = b1.result()
	b1.clear()
	
	assert(res1.size == 3)
	assert(res1.forall { _.isSymmetric })
	assert(res1.head == Pair(1, 1))
	assert(res1.last == Pair(3, 3))
	
	
	// Tests alternating +=
	
	b1.left += 1
	b1.right += 1
	b1.left += 2
	b1.right += 2
	
	val res2 = b1.result()
	b1.clear()
	
	assert(res2.size == 2)
	assert(res2.head == Pair(1, 1))
	assert(res2.last == Pair(2, 2))
	
	
	// Tests one-sided +=
	
	b1.left += 1
	b1.left += 2
	b1.right += 1
	b1.left += 3
	
	val res3 = b1.result()
	b1.clear()
	
	assert(res3.size == 1)
	assert(res3.head == Pair(1, 1))
	
	
	// Tests ++=
	
	b1.left ++= Vector(1, 2, 3)
	b1.right ++= Vector(1, 2)
	b1.right ++= Vector(3, 4, 5)
	b1.left ++= Vector(4, 5)
	
	val res4 = b1.result()
	b1.clear()
	
	assert(res4.size == 5)
	assert(res4.forall { _.isSymmetric })
	assert(res4.head == Pair(1, 1))
	assert(res4.last == Pair(5, 5))
	
	println("Success!")
}
