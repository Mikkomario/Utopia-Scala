package utopia.paradigm.test

import utopia.flow.operator.EqualsExtensions._
import utopia.paradigm.enumeration.Alignment._
import utopia.paradigm.enumeration.LinearAlignment.{Close, Far, Middle}
import utopia.paradigm.enumeration.{Alignment, LinearAlignment}
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.{Point, Size}

/**
  * Testing alignment handling
  * @author Mikko Hilpinen
  * @since 11.8.2022, v1.0
  */
object AlignmentTest extends App
{
	ParadigmDataType.setup()
	
	LinearAlignment.values.foreach { a => assert(a.opposite.opposite == a, s"$a opposite not correct") }
	
	val len = 10.0
	val area = 100.0
	
	assert(Close.position(len, area) == 0.0)
	assert(Middle.position(len, area) ~== 45.0)
	assert(Far.position(len, area) ~== 90.0)
	
	assert(Close.position(area, len) == 0.0)
	assert(Middle.position(area, len) ~== -45.0)
	assert(Far.position(area, len) ~== -90.0)
	
	Alignment.values.foreach { a => assert(Alignment(a.horizontal, a.vertical) == a, s"Alignment($a) not correct") }
	Alignment.values.foreach { a => assert(a.opposite.opposite == a, s"$a opposite not correct") }
	
	val s = Size.square(10.0)
	val a = Size.square(30.0)
	
	/*
	Alignment.values.foreach { al =>
		println(s"$al: ${al.origin(a)}")
	}*/
	
	assert(TopLeft.origin(a) ~== Point.origin)
	assert(Top.origin(a) ~== Point(15))
	assert(TopRight.origin(a) ~== Point(30))
	assert(Left.origin(a) ~== Point(0, 15))
	assert(Center.origin(a) ~== Point(15, 15))
	assert(Right.origin(a) ~== Point(30, 15))
	assert(BottomLeft.origin(a) ~== Point(0, 30))
	assert(Bottom.origin(a) ~== Point(15, 30))
	assert(BottomRight.origin(a) ~== Point(30, 30))
	
	assert(TopLeft.position(s, a) ~== Point.origin)
	assert(Top.position(s, a) ~== Point(10))
	assert(TopRight.position(s, a) ~== Point(20))
	assert(Left.position(s, a) ~== Point(0, 10))
	assert(Center.position(s, a) ~== Point(10, 10))
	assert(Right.position(s, a) ~== Point(20, 10))
	assert(BottomLeft.position(s, a) ~== Point(0, 20))
	assert(Bottom.position(s, a) ~== Point(10, 20))
	assert(BottomRight.position(s, a) ~== Point(20, 20))
	
	println("Success")
}
