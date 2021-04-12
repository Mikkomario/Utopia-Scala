package utopia.flow.test.time

import utopia.flow.time.TimeExtensions._
import utopia.flow.time.Today

/**
  * Tests min and max methods for local date
  * @author Mikko Hilpinen
  * @since 12.4.2021, v1.9
  */
object DateMinMaxTest extends App
{
	val d1 = Today.toLocalDate
	val d2 = d1.tomorrow
	
	assert((d1 min d2) == d1)
	assert((d1 max d2) == d2)
	
	println("Success!")
}
