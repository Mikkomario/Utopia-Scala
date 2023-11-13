package utopia.paradigm.measurement

import utopia.flow.operator.MayBeAboutZero
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.{Sign, SignOrZero, SignedOrZero}
import utopia.paradigm.measurement.DistanceUnit.{CentiMeter, Feet, Inch, KiloMeter, Meter, MilliMeter}

object Distance
{
	// ATTRIBUTES   -----------------
	
	/**
	 * A distance of 0m
	 */
	val zero = apply(0.0, Meter)
	
	
	// OTHER    ---------------------
	
	/**
	 * @param millis Amount of millimeters
	 * @return A distance
	 */
	def ofMillis(millis: Double) = Distance(millis, MilliMeter)
	/**
	 * @param centiMeters Amount of centimeters
	 * @return A distance
	 */
	def ofCm(centiMeters: Double) = Distance(centiMeters, CentiMeter)
	/**
	 * @param meters Amount of meters
	 * @return A distance
	 */
	def ofMeters(meters: Double) = Distance(meters, Meter)
	/**
	  * @param km Amount of kilometers (km)
	  * @return A distance matching that value
	  */
	def ofKilometers(km: Double) = Distance(km, KiloMeter)
	
	/**
	 * @param inches Amount of inhes
	 * @return A distance
	 */
	def ofInches(inches: Double) = Distance(inches, Inch)
	/**
	 * @param feet Amount of feet
	 * @return a distance
	 */
	def ofFeet(feet: Double) = Distance(feet, Feet)
	
	/**
	 * @param pixels Amount of pixels
	 * @param ppi Pixels per inch in current context (implicit)
	 * @return A distance
	 */
	def ofPixels(pixels: Double)(implicit ppi: Ppi) =
		if (ppi.value == 0) ofInches(0) else ofInches(pixels / ppi.value)
}

/**
 * Represents a physical distance
 * @author Mikko Hilpinen
 * @since Genesis 24.6.2020, v2.3
 */
case class Distance(amount: Double, unit: DistanceUnit)
	extends SelfComparable[Distance] with SignedOrZero[Distance] with MayBeAboutZero[Distance, Distance]
{
	// COMPUTED ---------------------
	
	/**
	 * @param ppi Pixels per inch in this context (implicit)
	 * @return This distance in pixels
	 */
	def toPixels(implicit ppi: Ppi) = amount * unit.toPixels
	
	/**
	 * @return This distance in millimeters
	 */
	def toMm = toUnit(MilliMeter)
	/**
	 * @return This distance in centimeters
	 */
	def toCm = toUnit(CentiMeter)
	/**
	 * @return This distance in meters
	 */
	def toM = toUnit(Meter)
	/**
	 * @return This distance in inches
	 */
	def toInches = toUnit(Inch)
	/**
	 * @return This distance in feet
	 */
	def toFeet = toUnit(Feet)
	
	
	// IMPLEMENTED  -----------------
	
	override def self = this
	
	override def sign: SignOrZero = Sign.of(amount)
	override def zero: Distance = Distance.zero
	override def isAboutZero: Boolean = amount ~== 0.0
	
	override def toString = s"$amount $unit"
	
	override def ~==(other: Distance): Boolean = amount ~== other.toUnit(unit)
	override def compareTo(o: Distance) = {
		val diff = amount - o.toUnit(unit)
		if (diff > 0)
			1
		else if (diff < 0)
			-1
		else
			0
	}
	
	
	// OTHER    ---------------------
	
	/**
	 * @param targetUnit Target unit
	 * @return Length of this distance in the target unit
	 */
	def toUnit(targetUnit: DistanceUnit) = amount * unit.conversionModifierFor(targetUnit)
	
	/**
	 * @return A negative copy of this distance
	 */
	def unary_- = copy(amount = -amount)
	
	/**
	 * @param other Another instance
	 * @return A combination of these distances
	 */
	def +(other: Distance) = copy(amount = amount + other.toUnit(unit))
	/**
	 * @param other Another instance
	 * @return A subtraction of these instances
	 */
	def -(other: Distance) = this + (-other)
	
	/**
	 * @param mod A modifier
	 * @return A multiplied copy of this distance
	 */
	def *(mod: Double) = copy(amount = amount * mod)
	
	/**
	 * @param div A divider
	 * @return A divided copy of this instance
	 */
	def /(div: Double) = copy(amount = amount / div)
	/**
	 * @param other Another distance
	 * @return Ratio between these two distances
	 */
	def /(other: Distance) = amount / other.toUnit(unit)
}
