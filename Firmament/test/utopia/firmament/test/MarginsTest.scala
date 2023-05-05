package utopia.firmament.test

import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.SizeCategory.{Large, Small, VeryLarge, VerySmall}
import utopia.flow.operator.EqualsExtensions._
import utopia.paradigm.transform.Adjustment

/**
  * Tests margins and adjustments
  * @author Mikko Hilpinen
  * @since 4.5.2023, v1.1
  */
object MarginsTest extends App
{
	implicit val halvingAdjustment: Adjustment = Adjustment(0.5)
	
	assert(halvingAdjustment.increase ~== 1.0)
	
	assert(halvingAdjustment(1) ~== 2.0, halvingAdjustment(1))
	assert(halvingAdjustment(-1) ~== 0.5, halvingAdjustment(-1))
	
	assert(Small.scaling ~== 0.5, Small.scaling)
	assert(VerySmall.scaling ~== 0.25, VerySmall.scaling)
	assert(Large.scaling ~== 2.0, Large.scaling)
	assert(VeryLarge.scaling ~== 4.0, VeryLarge.scaling)
	
	val m1 = Margins(10.0, halvingAdjustment)
	
	assert(m1.medium == 10.0)
	assert(m1.small ~== 5.0)
	assert(m1.large ~== 20.0)
	assert(m1.verySmall ~== 3.0, m1.verySmall) // Rounding
	assert(m1(VeryLarge) ~== 40.0)
	
	println("Success!")
}
