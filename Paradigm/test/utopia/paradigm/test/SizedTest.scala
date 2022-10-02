package utopia.paradigm.test

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.{Size, Vector2D}

/**
  * Tests size-altering functions
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.1
  */
object SizedTest extends App
{
	ParadigmDataType.setup()
	
	val s1 = Size.square(10)
	val s2 = Size(20, 30)
	val s3 = Size(5, 50)
	
	assert(s2.width == 20)
	assert(s2.height == 30)
	assert(s3.lengthAlong(X) == 5)
	assert(s3.lengthAlong(Y) == 50)
	
	assert(s2.fills(s1))
	assert(s2.fills(Y(25)))
	assert(!s3.fills(s1))
	assert(!s1.fills(s2))
	assert(!s1.fills(X(20)))
	
	assert(s1.fitsWithin(s2))
	assert(!s1.fitsWithin(s3))
	assert(s2.fitsWithin(X(20)))
	assert(!s2.fitsWithin(Y(10)))
	
	assert(s2.spans(X(20)))
	assert(s3.spans(Y(30)))
	assert(!s2.spans(Y(50)))
	
	assert(s1.withScaledSize(2) == Size.square(20))
	assert(s2.withScaledSize(0.5) == Size(10, 15))
	assert(s1.withScaledSize(Vector2D(1, 2)) == Size(10, 20))
	assert(s2.withDividedSize(2.0) == Size(10, 15))
	assert(s2.withDividedSize(Vector2D(2.0, 3.0)) == s1)
	
	assert(s1.withLength(X(20)) == Size(20, 10))
	assert(s1.withLength(Y(20), preserveShape = true) == Size(20, 20), s1.withLength(Y(20), preserveShape = true))
	assert(s1.withWidth(20, preserveShape = true) == Size(20, 20))
	
	assert(s1.filling(s2) == Size.square(30))
	assert(s1.filling(s3) == Size.square(50))
	assert(s2.filling(s1) == s2)
	assert(s3.filling(s1) == Size(10, 100))
	assert(s2.filling(s1, minimize = true) == Size(10, 15))
	
	assert(s1.fittingWithin(s2) == s1)
	assert(Size(20, 10).fittingWithin(s1) == Size(10, 5))
	assert(s1.fittingWithin(s2, maximize = true) == Size.square(20))
	
	assert(s1.spanning(X(20)) == Size.square(20))
	assert(s2.spanning(Y(20)) == s2)
	assert(s2.spanning(Y(15), minimize = true) == Size(10, 15))
	
	assert(s1.fittingWithin(X(5)) == Size.square(5))
	assert(s1.fittingWithin(Y(20)) == s1)
	
	assert(s1.croppedToFitWithin(X(5)) == Size(5, 10))
	assert(s1.croppedToFitWithin(X(20)) == s1)
	assert(s1.croppedToFitWithin(s2) == s1)
	assert(s3.croppedToFitWithin(s1) == Size(5, 10))
	
	println("Success")
}
