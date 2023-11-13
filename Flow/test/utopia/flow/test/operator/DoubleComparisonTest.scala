package utopia.flow.test.operator

import utopia.flow.time.Now
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.time.TimeExtensions._

/**
  * Tests double comparison speed
  * @author Mikko Hilpinen
  * @since 24.8.2023, v2.2
  */
object DoubleComparisonTest extends App
{
	val d1 = 0.003
	val d2 = 0.004
	
	val start = Now.toInstant
	(0 until 100000000).foreach { _ =>
		d1 ~== d2
	}
	val end = Now.toInstant
	
	println((end - start).description)
}
