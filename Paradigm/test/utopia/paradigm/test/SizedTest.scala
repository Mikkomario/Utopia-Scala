package utopia.paradigm.test

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.{Size, Sized}

/**
  * Used for testing size functions
  * @author Mikko Hilpinen
  * @since 28.11.2022, v1.2
  */
object SizedTest extends App
{
	ParadigmDataType.setup()
	
	case class SizeWrapper(size: Size) extends Sized[SizeWrapper]
	{
		override def repr = this
		override def withSize(size: Size) = copy(size)
	}
	
	val s = SizeWrapper(Size(10, 20))
	
	assert(s.fittingWithin(Y(5)).size == Size(2.5, 5.0))
	assert(s.withLength(X(2.5)).size == Size(2.5, 20.0))
	assert(s.withLength(X(2.5), preserveShape = true).size == Size(2.5, 5.0))
	
	println("Success!")
}
