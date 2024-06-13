package utopia.flow.test.collection

import utopia.flow.collection.immutable.{Empty, Single}

/**
  * Tests Empty
  * @author Mikko Hilpinen
  * @since 12.06.2024, v2.4
  */
object EmptyTest extends App
{
	//noinspection EmptyCheck
	assert(Empty.size == 0)
	assert(Empty.isEmpty)
	Empty.foreach { _ => throw new IllegalStateException("Failure state") }
	
	assert(Empty :+ 1 == Single(1))
	assert(1 +: Empty == Single(1))
	assert(Empty ++ Vector(1, 2, 3) == Vector(1, 2, 3))
	assert(Vector(1, 2, 3) ++ Empty == Vector(1, 2, 3))
	
	println(s"Success!")
}
