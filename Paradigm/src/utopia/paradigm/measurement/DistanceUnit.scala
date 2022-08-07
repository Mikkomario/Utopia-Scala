package utopia.paradigm.measurement

/**
 * An enumeration for standard distance units
 * @author Mikko Hilpinen
 * @since Genesis 24.6.2020, v2.3
 */
sealed trait DistanceUnit
{
	import DistanceUnit._
	
	// ABSTRACT ------------------------------
	
	/**
	 * @param targetUnit Another unit
	 * @return A modifier that must be applied to a number when converting from this unit to the other unit
	 */
	def conversionModifierFor(targetUnit: DistanceUnit): Double
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @param ppi Pixels per inch in this context (implicit)
	 * @return A modifier from this unit to pixels
	 */
	def toPixels(implicit ppi: Ppi) = toInch * ppi.value
	
	/**
	 * @return A modifier from this unit to millimeters
	 */
	def toMm = conversionModifierFor(MilliMeter)
	/**
	 * @return a modifier from this unit to centimeters
	 */
	def toCm = conversionModifierFor(CentiMeter)
	/**
	 * @return A modifier from this unit to meters
	 */
	def toM = conversionModifierFor(Meter)
	/**
	 * @return A modifier from this unit to kilometers
	 */
	def toKm = conversionModifierFor(KiloMeter)
	
	/**
	 * @return A modifier from this unit to inches
	 */
	def toInch = conversionModifierFor(Inch)
	/**
	 * @return A modifier from this unit to feet
	 */
	def toFeet = conversionModifierFor(Feet)
}

object DistanceUnit
{
	// TODO: Add a graph-based algorithm for unit conversions
	
	/**
	 * Millimeter length unit (1/1000 of a meter)
	 */
	case object MilliMeter extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case MilliMeter => 1.0
			case CentiMeter => 0.1
			case Meter => 0.001
			case _ => toCm * CentiMeter.conversionModifierFor(targetUnit)
		}
	}
	/**
	 * Centimeter length unit (1/100 of a meter)
	 */
	case object CentiMeter extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case CentiMeter => 1.0
			case MilliMeter => 10.0
			case Meter => 0.01
			case KiloMeter | NauticalMile => toM * Meter.conversionModifierFor(KiloMeter)
			case Inch => 1 / 2.54
			case _ => toInch * Inch.conversionModifierFor(targetUnit)
		}
	}
	/**
	 * Meter length unit (standard metric length)
	 */
	case object Meter extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case Meter => 1.0
			case MilliMeter => 0.001
			case CentiMeter => 0.01
			case KiloMeter => 0.001
			case Feet => 3.2808399
			case NauticalMile => toKm * KiloMeter.conversionModifierFor(NauticalMile)
			case _ => toFeet * Feet.conversionModifierFor(targetUnit)
		}
	}
	/**
	 * Kilometer length unit (1000 meters)
	 */
	case object KiloMeter extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case KiloMeter => 1.0
			case Meter => 1000.0
			case Mile => 0.621371192
			case NauticalMile => 0.539956803
			case _ => toM * Meter.conversionModifierFor(targetUnit)
		}
	}
	
	/**
	 * Inch length unit
	 */
	case object Inch extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case Inch => 1.0
			case Feet => 0.0833333333
			case Mile => toFeet * Feet.conversionModifierFor(targetUnit)
			case CentiMeter => 2.54
			case _ => toCm * CentiMeter.conversionModifierFor(targetUnit)
		}
	}
	/**
	 * Feet length unit (standard imperial length unit)
	 */
	case object Feet extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case Feet => 1.0
			case Inch => 12.0
			case Mile => 0.000189393939
			case Meter => 0.3048
			case _ => toM * Meter.conversionModifierFor(targetUnit)
		}
	}
	/**
	 * Mile length unit
	 */
	case object Mile extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case Mile => 1.0
			case Feet => 5280.0
			case KiloMeter => 1.609344
			case NauticalMile => 0.868976242
			case _ => toFeet * Feet.conversionModifierFor(targetUnit)
		}
	}
	
	/**
	 * Distance unit used in sailing. Exactly 1 852 metres.
	 */
	case object NauticalMile extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case NauticalMile => 1.0
			case Meter => 1852.0
			case KiloMeter => 1.852
			case Mile => 1.15077945
			case _ => toM * Meter.conversionModifierFor(targetUnit)
		}
	}
}
