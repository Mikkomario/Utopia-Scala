package utopia.paradigm.test

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.paradigm.measurement.Distance
import utopia.paradigm.measurement.MetricScale._

/**
  * Tests some distance conversions
  * @author Mikko Hilpinen
  * @since 5.9.2023, v1.4
  */
object DistanceTest extends App
{
	assert(Default.modifierTo(Deci) == 10, Default.modifierTo(Deci))
	assert(Deci.modifierTo(Default) == 0.1)
	assert(Deci.modifierTo(Milli) == 100)
	assert(Milli.modifierTo(Deci) == 0.01)
	assert(Mega.modifierTo(Kilo) == 1000)
	
	assert(Default.modifierFrom(Centi) == 0.01)
	assert(Centi.modifierFrom(Default) == 100)
	assert(Kilo.modifierFrom(Mega) == 1000)
	assert(Mega.modifierFrom(Kilo) == 0.001)
	
	val d = Distance.ofMeters(154.3)
	val d2 = Distance.ofCm(15.0)
	val d3 = d - d2
	
	assert(d.toM ~== 154.3)
	assert(d.toCm ~== 15430.0, d.toCm)
	assert(d2.toM ~== 0.15)
	assert(d3.toM ~== 154.15)
	
	println("Done!")
}
