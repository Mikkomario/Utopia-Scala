package utopia.paradigm.measurement

import utopia.paradigm.measurement.DistanceUnit.{CentiMeter, MicroMeter, MilliMeter}

/**
  * Common trait for elements which, when combined with a distance unit,
  * can be converted into their real sized counterparts
  * @tparam Real Real-sized version of this class
  * @author Mikko Hilpinen
  * @since 02.01.2025, v1.7.1
  */
trait DistanceConvertible[+Real]
{
	// ABSTRACT --------------------------
	
	/**
	  * @param unit A unit in which this instance's measurements are given
	  * @return A copy of this instance with real world lengths
	  */
	def in(unit: DistanceUnit): Real
	
	
	// COMPUTED -------------------------
	
	def microMeters = in(MicroMeter)
	def milliMeters = in(MilliMeter)
	def centiMeters = in(CentiMeter)
	// def deci
}
