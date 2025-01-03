package utopia.paradigm.measurement

import utopia.flow.operator.MayBeAboutZero
import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.{Sign, SignOrZero, SignedOrZero}
import utopia.paradigm.measurement.DistanceUnit.{CentiMeter, Dtp, Feet, Inch, KiloMeter, Meter, MeterUnit, Mile, MilliMeter, NauticalMile}
import utopia.paradigm.transform.LinearSizeAdjustable

import scala.util.{Failure, Success, Try}

object Distance
{
	// ATTRIBUTES   -----------------
	
	/**
	 * A distance of 0m
	 */
	val zero = apply(0.0, Meter)
	
	/**
	  * An equality function that checks for approximate distance equality
	  */
	implicit val approxEquals: EqualsFunction[Distance] = (d1, d2) => d1.amount ~== d2.toUnit(d1.unit)
	
	
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
	 * @param inches Amount of inches
	 * @return A distance
	 */
	def ofInches(inches: Double) = Distance(inches, Inch)
	/**
	 * @param feet Amount of feet
	 * @return a distance
	 */
	def ofFeet(feet: Double) = Distance(feet, Feet)
	/**
	  * @param miles Amount of miles
	  * @return A distance
	  */
	def ofMiles(miles: Double) = Distance(miles, Mile)
	/**
	  * @param nauticalMiles Amount of nautical miles
	  * @return A distance
	  */
	def ofNauticalMiles(nauticalMiles: Double) = Distance(nauticalMiles, NauticalMile)
	
	/**
	  * @param points Amount of typographic points
	  * @return A distance
	  */
	def ofPoints(points: Double) = Distance(points, Dtp)
	
	/**
	 * @param pixels Amount of pixels
	 * @param ppi Pixels per inch in current context (implicit)
	 * @return A distance
	 */
	def ofPixels(pixels: Double)(implicit ppi: Ppi) =
		if (ppi.value == 0) ofInches(0) else ofInches(pixels / ppi.value)
	
	/**
	  * Parses a distance from a string
	  * @param str A string combining amount and unit. E.g. "3.2km".
	  * @param defaultUnit Unit that will be assigned if no other unit has been specified
	  * @return Distance parsed from the specified string.
	  */
	def parse(str: String, defaultUnit: => DistanceUnit) = {
		val unitStartIndex = str.lastIndexWhere { _.isDigit } + 1
		if (unitStartIndex < str.length)
			Try { str.take(unitStartIndex).trim.toDouble }.flatMap { amount =>
				val unitStr = str.drop(unitStartIndex).trim
				DistanceUnit.values.find { _.abbreviation ~== unitStr } match {
					case Some(unit) => Success(Distance(amount, unit))
					case None => Failure(new IllegalArgumentException(s"Unrecognized distance unit '$unitStr'"))
				}
			}
		else
			Try { str.toDouble }.map { Distance(_, defaultUnit) }
	}
		
	
	// NESTED   ---------------------
	
	/**
	  * A numeric implementation for distances, assuming a specific unit of measurement
	  * @param unit Assumed unit of measurement
	  */
	case class DistanceIsFractionalIn(unit: DistanceUnit) extends Fractional[Distance]
	{
		override def plus(x: Distance, y: Distance): Distance = x + y
		override def minus(x: Distance, y: Distance): Distance = x - y
		override def times(x: Distance, y: Distance): Distance = x * y
		override def div(x: Distance, y: Distance): Distance = Distance(x/y, x.unit)
		
		override def negate(x: Distance): Distance = -x
		
		override def fromInt(x: Int): Distance = Distance(x, unit)
		override def parseString(str: String): Option[Distance] = {
			val unitStartIndex = str.lastIndexWhere { _.isLetter }
			if (unitStartIndex >= 0)
				str.take(unitStartIndex).trim.toDoubleOption.flatMap { amount =>
					val unitStr = str.drop(unitStartIndex).trim
					DistanceUnit.values.find { _.abbreviation ~== unitStr }.map { Distance(amount, _) }
				}
			else
				str.toDoubleOption.map { Distance(_, unit) }
		}
		
		override def toInt(x: Distance): Int = toDouble(x).toInt
		override def toLong(x: Distance): Long = toDouble(x).toLong
		override def toFloat(x: Distance): Float = toDouble(x).toFloat
		override def toDouble(x: Distance): Double = x.toUnit(unit)
		
		override def compare(x: Distance, y: Distance): Int = x.compareTo(y)
	}
}

/**
 * Represents a physical distance
 * @author Mikko Hilpinen
 * @since Genesis 24.6.2020, v2.3
 */
case class Distance(amount: Double, unit: DistanceUnit)
	extends SelfComparable[Distance] with SignedOrZero[Distance] with MayBeAboutZero[Distance, Distance]
		with LinearSizeAdjustable[Distance] with Combinable[Distance, Distance]
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
	  * @return This distance in meters
	  */
	def toMeters = toM
	/**
	 * @return This distance in inches
	 */
	def toInches = toUnit(Inch)
	/**
	 * @return This distance in feet
	 */
	def toFeet = toUnit(Feet)
	/**
	  * @return This distance in miles
	  */
	def toMiles = toUnit(Mile)
	/**
	  * @return This distance in typographic points
	  */
	def toPoints = toUnit(Dtp)
	
	
	// IMPLEMENTED  -----------------
	
	override def self = this
	
	override def sign: SignOrZero = Sign.of(amount)
	override def zero: Distance = Distance.zero
	override def isAboutZero: Boolean = amount ~== 0.0
	
	/**
	  * @return A negative copy of this distance
	  */
	override def unary_- = copy(amount = -amount)
	
	override def toString = unit match {
		case _: MeterUnit =>
			val targetUnit = MeterUnit.appropriateFor(toM)
			s"${toUnit(targetUnit)} $targetUnit"
		case u => s"$amount $u"
	}
	
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
	
	/**
	  * @param other Another instance
	  * @return A combination of these distances
	  */
	override def +(other: Distance) = copy(amount = amount + other.toUnit(unit))
	/**
	  * @param mod A modifier
	  * @return A multiplied copy of this distance
	  */
	override def *(mod: Double) = copy(amount = amount * mod)
	/**
	  * @param div A divider
	  * @return A divided copy of this instance
	  */
	override def /(div: Double) = copy(amount = amount / div)
	
	
	// OTHER    ---------------------
	
	/**
	 * @param targetUnit Target unit
	 * @return Length of this distance in the target unit
	 */
	def toUnit(targetUnit: DistanceUnit) = amount * unit.conversionModifierTo(targetUnit)
	
	/**
	 * @param other Another instance
	 * @return A subtraction of these instances
	 */
	def -(other: Distance) = this + (-other)
	/**
	 * @param other Another distance
	 * @return Ratio between these two distances
	 */
	def /(other: Distance) = amount / other.toUnit(unit)
}
