package utopia.flow.test.operator

import utopia.flow.operator.UncertainSign
import utopia.flow.util.UncertainNumber.{UncertainInt, UncertainNumberRange}
import utopia.flow.util.{UncertainBoolean, UncertainNumber}

/**
  * @author Mikko Hilpinen
  * @since 19.8.2023, v2.2
  */
object UncertainNumbersTest extends App
{
	val n1: UncertainInt = 1
	val n2: UncertainInt = 2
	val any = UncertainNumber.any[Int]
	val nr1to3 = UncertainNumberRange(1, 3)
	val nrm5to2 = UncertainNumberRange(-5, 2)
	val pos = UncertainNumber.positive[Int]
	val neg = UncertainNumber.negative[Int]
	val gt3 = UncertainNumber.greaterThan(3)
	val lteq3 = UncertainNumber.lessThan(3, orEqual = true)
	
	def assertFalse(uncertain: UncertainBoolean) = assert(uncertain.isCertainlyFalse, uncertain)
	def assertTrue(uncertain: UncertainBoolean) = assert(uncertain.isCertainlyTrue, uncertain)
	def assertUncertain(uncertain: UncertainBoolean) = assert(uncertain == UncertainBoolean, uncertain)
	
	// Tests ==
	assert((n1 == 1).isCertainlyTrue)
	assert((n1 == 2).isCertainlyFalse)
	assert((n2 == 1).isCertainlyFalse)
	assert((n2 == 2).isCertainlyTrue)
	assert((any == 1).isUncertain)
	assert((nr1to3 == 1).isUncertain)
	assert((nr1to3 == 4).isCertainlyFalse)
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
	assert(nr1to3.sign.isPositive.isCertainlyTrue)
	assert(nrm5to2.sign == UncertainSign)
	assert(pos.sign.isPositive.isCertainlyTrue)
	assert(neg.sign.isNegative.isCertainlyTrue)
	assert(gt3.sign.isPositive.isCertainlyTrue)
	assert(lteq3.sign == UncertainSign)
	
	// Tests == with other uncertain values
	assert((n1 == n2).isCertainlyFalse)
	assert((n1 == any).isUncertain)
	assert((n1 == nr1to3).isUncertain)
	assert((n1 == pos).isUncertain)
	assert((n1 == neg).isCertainlyFalse)
	assert((n1 == gt3).isCertainlyFalse)
	assert((n1 == lteq3).isUncertain)
	assert((nr1to3 == nrm5to2).isUncertain)
	assert((nr1to3 == pos).isUncertain)
	assert((nr1to3 == neg).isCertainlyFalse)
	assert((nrm5to2 == neg).isUncertain)
	assert((nr1to3 == gt3).isCertainlyFalse)
	assert((nr1to3 == lteq3).isUncertain)
	assert((gt3 == pos).isUncertain)
	assert((gt3 == neg).isCertainlyFalse)
	assert((lteq3 == pos).isUncertain)
	assert((lteq3 == neg).isUncertain)
	assert((gt3 == lteq3).isCertainlyFalse)
	
	// Tests <, <=, > and >=
	assertTrue(n1 >= 1)
	assertTrue(n1 > -2)
	assertFalse(n1 < 0)
	assertTrue(pos > -2)
	assertFalse(pos < -2)
	assertUncertain(pos > 3)
	assertUncertain(pos < 3)
	assertTrue(pos > 0)
	assertTrue(neg < 2)
	assertFalse(neg > 2)
	assertUncertain(neg < -3)
	assertTrue(nr1to3 > -2)
	assertUncertain(nr1to3 > 1)
	assertFalse(nr1to3 > 3)
	assertTrue(gt3 > 2)
	assertTrue(gt3 > 3)
	assertFalse(gt3 < 0)
	assertUncertain(gt3 < 10)
	assertTrue(lteq3 <= 3)
	assertUncertain(lteq3 <= 2)
	
	assertUncertain(n1 >= pos)
	assertUncertain(n2 < pos)
	assertTrue(n1 > neg)
	assertFalse(n1 > nr1to3)
	assertTrue(n1 <= nr1to3)
	assertUncertain(n2 > nr1to3)
	assertUncertain(n2 >= nr1to3)
	assertUncertain(n2 < nr1to3)
	assertTrue(n1 < gt3)
	assertTrue(neg < pos)
	assertTrue(pos > neg)
	assertUncertain(pos >= gt3)
	assertTrue(neg <= gt3)
	assertUncertain(neg <= lteq3)
	assertUncertain(nrm5to2 < nr1to3)
	assertTrue(gt3 > nr1to3)
	assertTrue(gt3 > lteq3)
	assertFalse(gt3 <= lteq3)
	
	// Tests min & max value
	assert(n1.largestPossibleValue.contains(1))
	assert(n1.smallestPossibleValue.contains(1))
	assert(pos.smallestPossibleValue.contains(0))
	assert(pos.largestPossibleValue.isEmpty)
	assert(neg.smallestPossibleValue.isEmpty)
	assert(neg.largestPossibleValue.contains(0))
	assert(nr1to3.smallestPossibleValue.contains(1))
	assert(nr1to3.largestPossibleValue.contains(3))
	assert(gt3.smallestPossibleValue.contains(3))
	assert(gt3.largestPossibleValue.isEmpty)
	assert(lteq3.smallestPossibleValue.isEmpty)
	assert(lteq3.largestPossibleValue.contains(3))
	assert(any.smallestPossibleValue.isEmpty)
	assert(any.largestPossibleValue.isEmpty)
	
	// Tests - (unary)
	val rn1 = -n1
	val rpos = -pos
	val rnr1 = -nr1to3
	val rgt3 = -gt3
	
	assert(rn1.exact.contains(-1))
	assert(rpos.isCertainlyNegative)
	assert(rnr1.isCertainlyNegative)
	assert(rnr1.largestPossibleValue.contains(-1))
	assert(rnr1.smallestPossibleValue.contains(-3))
	assert(rgt3.largestPossibleValue.contains(-3))
	assert(rgt3.isCertainlyNegative)
	
	// Tests +
	val n3 = n1 + n2
	val gt1 = n1 + pos
	val lt1 = n1 + neg
	val nr3to5 = nr1to3 + n2
	val nrm4to5 = nr1to3 + nrm5to2
	val lt3 = nr1to3 + neg
	val pospos = pos + pos
	val posneg = pos + neg
	val gt4 = gt3 + n1
	val lteq6 = lteq3 + nr1to3
	val gt3pos = gt3 + pos
	
	assert(n3.exact.contains(3))
	assert(gt1.smallestPossibleValue.contains(1))
	assertFalse(gt1 == 1)
	assert(gt1.largestPossibleValue.isEmpty)
	assert(lt1.largestPossibleValue.contains(1))
	assertFalse(lt1 == 1)
	assert(lt1.smallestPossibleValue.isEmpty)
	assert(nr3to5.smallestPossibleValue.contains(3))
	assert(nr3to5.largestPossibleValue.contains(5))
	assert(nrm4to5.smallestPossibleValue.contains(-4))
	assert(nrm4to5.largestPossibleValue.contains(5))
	assert(lt3.largestPossibleValue.contains(3), lt3)
	assert(lt3.smallestPossibleValue.isEmpty)
	assertFalse(lt3 == 3)
	assert(pospos.largestPossibleValue.isEmpty)
	assert(pospos.isCertainlyPositive)
	assert(posneg.sign == UncertainSign)
	assert(gt4.smallestPossibleValue.contains(4))
	assert(gt4.largestPossibleValue.isEmpty)
	assertFalse(gt4 == 4)
	assert(lteq6.smallestPossibleValue.isEmpty)
	assert(lteq6.largestPossibleValue.contains(6))
	assertUncertain(lteq6 == 6)
	assert(gt3pos.smallestPossibleValue.contains(3))
	
	// Tests abs
	val absn1 = n1.abs
	val nr0to5 = nrm5to2.abs
	val absneg = neg.abs
	val abslteq3 = lteq3.abs
	val absany = any.abs
	
	assert(absn1.exact.contains(1))
	assert(nr0to5.smallestPossibleValue.contains(0))
	assert(nr0to5.largestPossibleValue.contains(5))
	assert(absneg.isCertainlyPositive)
	assert(abslteq3.largestPossibleValue.isEmpty)
	assert(abslteq3.smallestPossibleValue.contains(0))
	assertUncertain(abslteq3 == 0)
	assert(absany.isCertainlyNotNegative)
	assertUncertain(absany == 0)
	
	// Tests min & max
	val nr2to3 = nr1to3.max(2)
	val gteq7 = pos.max(7)
	val gteq8 = gt3.max(8)
	val gteq9 = any.max(9)
	
	assert(n1.max(7).exact.contains(7))
	assert(n1.max(0).exact.contains(1))
	assert(nr1to3.max(7).exact.contains(7))
	assert(nr2to3.smallestPossibleValue.contains(2))
	assert(nr2to3.largestPossibleValue.contains(3))
	assert(nr1to3.max(0).smallestPossibleValue.contains(1))
	assert(gteq7.smallestPossibleValue.contains(7))
	assert(gteq7.largestPossibleValue.isEmpty)
	assertUncertain(gteq7 == 7)
	assert(pos.max(-3).smallestPossibleValue.contains(0), pos.max(-3))
	assert(gteq8.smallestPossibleValue.contains(8))
	assertUncertain(gteq8 == 8)
	assert(gt3.max(1).smallestPossibleValue.contains(3))
	assert(gteq9.smallestPossibleValue.contains(9), gteq9)
	assertUncertain(gteq9 == 9)
	
	println("Done!")
}
