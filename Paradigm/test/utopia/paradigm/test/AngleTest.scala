package utopia.paradigm.test

import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.generic.ParadigmDataType

/**
  * Tests functions for rotation and angle
  * @author Mikko Hilpinen
  * @since 25.08.2024, v1.7
  */
object AngleTest extends App
{
	ParadigmDataType.setup()
	
	assert(Angle.degrees(90) - Angle.zero ~== Rotation.degrees(90).clockwise, Angle.degrees(90) - Angle.zero)
	assert(Angle.degrees(120) - Angle.zero ~== Rotation.degrees(120).clockwise)
	assert(Angle.degrees(270) - Angle.zero ~== Rotation.degrees(90).counterclockwise)
	assert(Angle.degrees(240) - Angle.zero ~== Rotation.degrees(120).counterclockwise)
	
	assert(Angle.degrees(130) - Angle.degrees(200) ~== Rotation.degrees(70).counterclockwise)
	
	println(Angle.degrees(300) - Angle.degrees(7))
	
	/*
	Directions are: [144.93 degrees, 350.96 degrees, 305.58 degrees, 152.92 degrees, 201.25 degrees]
Paired: [Pair(201.25 degrees, 144.93 degrees), Pair(144.93 degrees, 350.96 degrees), Pair(350.96 degrees, 305.58 degrees), Pair(305.58 degrees, 152.92 degrees), Pair(152.92 degrees, 201.25 degrees)]
Direction = Counterclockwise
Rotations: [56.32 degrees Counterclockwise, 153.97 degrees Counterclockwise, 45.38 degrees Counterclockwise, 152.65 degrees Counterclockwise, 48.33 degrees Clockwise]
	 */
	// This should be a very sharp turn
	println(Angle.degrees(350) - Angle.degrees(145))
	
	println("Success!")
}
