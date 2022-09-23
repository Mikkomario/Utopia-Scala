package utopia.flow.test.datastructure

import utopia.flow.collection.value.iterable.DeepMap

/**
  * Tests DeepMap data structure
  * @author Mikko Hilpinen
  * @since 25.12.2021, v1.14.1
  */
object DeepMapTest extends App
{
	val m1 = DeepMap(
		Vector(1, 2, 3) -> "a",
		Vector(1, 2, 4) -> "b",
		Vector(1, 3, 3) -> "c",
		Vector(1, 1) -> "d",
		Vector(100) -> "e")
	
	assert(m1(1, 2, 3) == "a")
	assert(m1(1, 2, 4) == "b")
	assert(m1(1, 3, 3) == "c")
	assert(m1(1, 1) == "d")
	assert(m1(100) == "e")
	
	assert(m1.get(1, 2, 3).contains("a"))
	assert(m1.get(1, 2, 5).isEmpty)
	assert(m1.get(1).isEmpty)
	
	val m2 = m1 + (Vector(1, 2, 3) -> "x")
	val m3 = m1 + (Vector(1, 2, 5) -> "x")
	
	assert(m2(1, 2, 3) == "x")
	assert(m3(1, 2, 5) == "x")
	assert(m2(1, 2, 4) == "b")
	assert(m3(1, 2, 3) == "a")
	
	val m4 = DeepMap.flat(1 -> "x", 2 -> "y", 3 -> "z")
	
	assert(m4(1) == "x")
	assert(m4(2) == "y")
	
	val m5 = m1 ++ m4
	
	assert(m5(1) == "x")
	assert(m5(1, 2, 3) == "x")
	assert(m5(3) == "z")
	assert(m5(100) == "e")
	
	val m6 = m1 ++ (Vector(1, 2) -> m4)
	
	assert(m6(1, 2, 3) == "z")
	assert(m6(1, 2, 4) == "b")
	assert(m6(1, 3, 3) == "c")
	assert(m6(1, 2, 1) == "x")
	
	val m7 = m1 - 1
	val m8 = m1 - 100
	
	assert(m7.get(1, 2, 3).isEmpty)
	assert(m7(100) == "e")
	assert(m8.get(100).isEmpty)
	assert(m8(1, 2, 3) == "a")
	
	val m9 = m1 - Vector(1, 2, 3)
	
	assert(m9.get(1, 2, 3).isEmpty)
	assert(m9(1, 2, 4) == "b")
	
	println("Done!")
}
