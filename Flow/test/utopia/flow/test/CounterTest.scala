package utopia.flow.test

import utopia.flow.collection.mutable.iterator.{Counter, Generator}

/**
  * Tests Counter class
  * @author Mikko Hilpinen
  * @since 17.4.2019
  */
object CounterTest extends App
{
	val stringGen = Generator[String]("A") { _ + "A" }
	
	assert(stringGen.next() == "A")
	assert(stringGen.next() == "AA")
	assert(stringGen.next() == "AAA")
	
	val counter = new Counter(1, 1)
	
	assert(counter.next() == 1)
	assert(counter.next() == 2)
	assert(counter.next() == 3)
	
	println("Success")
}
