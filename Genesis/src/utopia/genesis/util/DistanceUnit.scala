package utopia.genesis.util

/**
 * An enumeration for standard distance units
 * @author Mikko Hilpinen
 * @since 24.6.2020, v2.3
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
	 * @return A modifier from this unit to pixels in the current screen
	 */
	def toScreenPixels = toPixels(Screen.ppi)
	
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
	/**
	 * Millimeter length unit
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
	 * Centimeter length unit
	 */
	case object CentiMeter extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case CentiMeter => 1.0
			case MilliMeter => 10.0
			case Meter => 0.01
			case Inch => 1 / 2.54
			case _ => toInch * Inch.conversionModifierFor(targetUnit)
		}
	}
	
	/**
	 * Meter length unit
	 */
	case object Meter extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case Meter => 1.0
			case CentiMeter => 0.01
			case MilliMeter => 0.001
			case Feet => 3.2808399
			case _ => toFeet * Feet.conversionModifierFor(targetUnit)
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
			case CentiMeter => 2.54
			case _ => toCm * CentiMeter.conversionModifierFor(targetUnit)
		}
	}
	
	/**
	 * Feet length unit
	 */
	case object Feet extends DistanceUnit
	{
		override def conversionModifierFor(targetUnit: DistanceUnit) = targetUnit match
		{
			case Feet => 1.0
			case Inch => 12.0
			case Meter => 0.3048
			case _ => toM * Meter.conversionModifierFor(targetUnit)
		}
	}
}
