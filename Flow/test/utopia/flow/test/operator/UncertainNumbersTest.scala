package utopia.flow.test.operator

import utopia.flow.operator.UncertainSign
import utopia.flow.util.UncertainNumber
import utopia.flow.util.UncertainNumber.{UncertainInt, UncertainNumberRange}

/**
  * @author Mikko Hilpinen
  * @since 19.8.2023, v2.2
  */
object UncertainNumbersTest extends App
{
	val n1: UncertainInt = 1
	val n2: UncertainInt = 2
	val any = UncertainNumber.any[Int]
	val nr1 = UncertainNumberRange(1, 3)
	val nr2 = UncertainNumberRange(-5, 2)
	val pos = UncertainNumber.positive[Int]
	val neg = UncertainNumber.negative[Int]
	val gt3 = UncertainNumber.greaterThan(3)
	val lteq3 = UncertainNumber.lessThan(3, orEqual = true)
	
	// Tests ==
	assert((n1 == 1).isCertainlyTrue)
	assert((n1 == 2).isCertainlyFalse)
	assert((n2 == 1).isCertainlyFalse)
	assert((n2 == 2).isCertainlyTrue)
	assert((any == 1).isUncertain)
	assert((nr1 == 1).isUncertain)
	assert((nr1 == 4).isCertainlyFalse)
	assert((pos == 3).isUncertain)
	assert((pos == 0).isCertainlyFalse)
	assert((pos == -3).isCertainlyFalse)
	assert((neg == 3).isCertainlyFalse)
	assert((neg == 0).isCertainlyFalse)
	assert((neg == -3).isUncertain)
	assert((gt3 == 4).isUncertain)
	assert((gt3 == 3).isCertainlyFalse)
	assert((gt3 == -3).isCertainlyFalse)
	assert((lteq3 == -3).isUncertain)
	assert((lteq3 == 3).isUncertain)
	
	// Tests sign
	assert(n1.sign.isPositive.isCertainlyTrue)
	assert(n2.sign.isPositive.isCertainlyTrue)
	assert(any.sign == UncertainSign)
	assert(nr1.sign.isPositive.isCertainlyTrue)
	assert(nr2.sign == UncertainSign)
	assert(pos.sign.isPositive.isCertainlyTrue)
	assert(neg.sign.isNegative.isCertainlyTrue)
	assert(gt3.sign.isPositive.isCertainlyTrue)
	assert(lteq3.sign == UncertainSign)
	
	// Tests == with other uncertain values
	assert((n1 == n2).isCertainlyFalse)
	assert((n1 == any).isUncertain)
	assert((n1 == nr1).isUncertain)
	assert((n1 == pos).isUncertain)
	assert((n1 == neg).isCertainlyFalse)
	assert((n1 == gt3).isCertainlyFalse)
	assert((n1 == lteq3).isUncertain)
	assert((nr1 == nr2).isUncertain)
	assert((nr1 == pos).isUncertain)
	assert((nr1 == neg).isCertainlyFalse)
	assert((nr2 == neg).isUncertain)
	assert((nr1 == gt3).isCertainlyFalse)
	assert((nr1 == lteq3).isUncertain)
	assert((gt3 == pos).isUncertain)
	assert((gt3 == neg).isCertainlyFalse)
	assert((lteq3 == pos).isUncertain)
	assert((lteq3 == neg).isUncertain)
	assert((gt3 == lteq3).isCertainlyFalse)
	
	println("Done!")
}
