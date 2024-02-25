package utopia.paradigm.test

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.paradigm.enumeration.FillAreaLogic.Fit
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * Tests various fill area -logic implementations
  * @author Mikko Hilpinen
  * @since 25/02/2024, v1.6
  */
object FillAreaLogicTest extends App
{
	private val smaller = Size(20, 15)
	private val larger = Size(30, 10)
	
	// Tests Fit
	private val lFitS = Fit(larger, smaller)
	
	assert(lFitS.width ~== 20.0)
	assert(lFitS.height ~== 6.666666666)
	assert(lFitS.position.x ~== 0.0)
	assert(lFitS.position.y ~== 4.1666666666)
	
	private val sFitL = Fit(smaller, larger)
	
	assert(sFitL.height ~== 10.0)
	assert(sFitL.width ~== 13.33333333)
	assert(sFitL.position.y ~== 0.0)
	assert(sFitL.position.x ~== 8.3333333333)
	
	println("Success!")
}
