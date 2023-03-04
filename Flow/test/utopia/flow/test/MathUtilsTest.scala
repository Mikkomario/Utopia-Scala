package utopia.flow.test

import utopia.flow.util.NumberExtensions._

/**
  * Tests number and math utility functions
  * @author Mikko Hilpinen
  * @since 4.3.2023, v2.1
  */
object MathUtilsTest extends App
{
	assert(1.2345.roundDecimals(2) == 1.23)
	assert(1.2345.roundDecimals(3) == 1.235)
	assert(1.2.roundDecimals(2) == 1.2)
	
	println("Success")
}
