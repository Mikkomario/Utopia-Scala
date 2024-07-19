package utopia.paradigm.test

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.{Color, Hsl}
import utopia.paradigm.generic.ParadigmDataType

/**
  * Tests certain color functions
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.6
  */
object ColorTest extends App
{
	ParadigmDataType.setup()
	
	Iterator
		.fill(100) {
			val inputs = Pair.fill[Color] { Hsl(Angle.random) }
			val avg = Color.average(inputs)
			assert(avg.values.forall { case (_, v) => v >= 0 && v <= 255 }, s"Avg of $inputs is $avg")
		}
	
	val colorSet = Set(Hsl(Angle.degrees(21.06)), Hsl(Angle.degrees(280)))
	val c = Color.average(Set(Hsl(Angle.degrees(21.06)), Hsl(Angle.degrees(280))))
	assert(c.values.forall { case (_, v) => v >= 0 && v <= 255 }, s"Avg of $colorSet is $c")
	
	println(c)
}
