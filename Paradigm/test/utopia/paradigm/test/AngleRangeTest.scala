package utopia.paradigm.test

import utopia.paradigm.angular.{Angle, AngleRange, Rotation}
import utopia.paradigm.generic.ParadigmDataType

/**
  * Tests certain AngleRange features
  * @author Mikko Hilpinen
  * @since 28.08.2024, v1.7
  */
object AngleRangeTest extends App
{
	ParadigmDataType.setup()
	
	// val res = apply(start, (end - start).towardsPreservingEndAngle(direction))
	// Start = 114.72 degrees; End = 137.18 degrees; Direction = Counterclockwise; Rotation = 360.00 degrees Counterclockwise
	
	val a1 = Angle.degrees(115)
	val a2 = Angle.degrees(140)
	val r1 = a2 - a1
	
	assert(r1 ~== Rotation.degrees(25).clockwise, r1)
	assert(r1.complementary ~== Rotation.degrees(335).counterclockwise, r1.complementary)
	
	val ar1 = AngleRange(a1, r1)
	val ar2 = AngleRange(a1, r1.complementary)
	
	assert(ar1.contains(Angle.degrees(120)))
	assert(!ar2.contains(Angle.degrees(120)))
	assert(ar2.contains(Angle.degrees(220)), ar2)
	assert(!ar1.contains(Angle.degrees(220)))
	
	println("Success!")
}
