package utopia.flow.test.time

import utopia.flow.time.TimeExtensions._

import java.time.LocalDate

/**
  * Performs some (not all) date-related operations
  * @author Mikko Hilpinen
  * @since 9.9.2021, v1.11.1
  */
object DateOperationsTest extends App
{
	val date1 = LocalDate.of(2021, 9, 9)
	val date2 = LocalDate.of(2021, 9, 12)
	
	assert(date2.toEpochDay - date1.toEpochDay == 3)
	assert((date2 - date1).length == 3)
	
	println("Success!")
}
