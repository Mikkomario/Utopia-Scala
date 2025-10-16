package utopia.flow.test.time

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.time.TimeUnit.{Day, Hour, MicroSecond, MilliSecond, Second, Week}

/**
 * Tests some TimeUnit functions
 * @author Mikko Hilpinen
 * @since 16.10.2025, v2.7
 */
object TimeUnitTest extends App
{
	assert(MilliSecond.countIn(2, Second) == 2000)
	assert(Second.countIn(2500, MilliSecond) == 2)
	
	assert(MilliSecond.countPreciselyIn(2.1, Second) ~== 2100.0 )
	assert(Second.countPreciselyIn(2500, MilliSecond) ~== 2.5)
	
	assert(Hour.countIn(1, Week) == 24 * 7)
	assert(Week.countIn(15, Day) == 2)
	
	assert(MilliSecond.countRemainderFrom(1234, MilliSecond, Second) == 234,
		MilliSecond.countRemainderFrom(1234, MilliSecond, Second))
	assert(MicroSecond.countRemainderFrom(1234, MilliSecond, Second) == 234000)
	assert(Hour.countRemainderFrom(15, Day, Week) == 24)
	
	println("Success!")
}
