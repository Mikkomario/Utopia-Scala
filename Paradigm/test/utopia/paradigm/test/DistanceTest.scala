package utopia.paradigm.test

import utopia.flow.operator.EqualsExtensions._
import utopia.paradigm.measurement.Distance

/**
  * Tests some distance conversions
  * @author Mikko Hilpinen
  * @since 5.9.2023, v1.4
  */
object DistanceTest extends App
{
	val d = Distance.ofMeters(154.3)
	val d2 = Distance.ofCm(15.0)
	val d3 = d - d2
	
	assert(d.toM ~== 154.3)
	assert(d.toCm ~== 15430.0, d.toCm)
	assert(d2.toM ~== 0.15)
	assert(d3.toM ~== 154.15)
	
	println("Done!")
}
