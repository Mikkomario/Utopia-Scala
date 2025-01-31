package utopia.flow.test.event

import utopia.flow.view.mutable.eventful.AssignableOnce
import utopia.flow.test.TestContext._

import scala.util.Try

/**
  * Testing SettableOnce -class
  * @author Mikko Hilpinen
  * @since 17.11.2022, v2.0
  */
object AssignableOnceTest extends App
{
	val s = AssignableOnce[Int]()
	
	assert(s.value.isEmpty)
	
	s.value = Some(1)
	
	assert(s.value.contains(1))
	assert(!s.trySet(2))
	
	assert(Try { s.value = Some(3) }.isFailure)
	
	println("Success!")
}
