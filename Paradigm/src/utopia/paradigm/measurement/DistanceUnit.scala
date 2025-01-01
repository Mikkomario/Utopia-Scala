package utopia.paradigm.measurement

import MetricScale._

/**
 * An enumeration for standard distance units
 * @author Mikko Hilpinen
 * @since Genesis 24.6.2020, v2.3
 */
// TODO: Add support for point units
sealed trait DistanceUnit
{
	import DistanceUnit._
	
	// ABSTRACT ------------------------------
	
	/**
	  * @return The abbreviation used for this unit
	  */
	def abbreviation: String
	
	/**
	 * @param targetUnit Another unit
	 * @return A modifier that must be applied to a number when converting from this unit to the other unit
	 */
	def conversionModifierTo(targetUnit: DistanceUnit): Double
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @param ppi Pixels per inch in this context (implicit)
	 * @return A modifier from this unit to pixels
	 */
	def toPixels(implicit ppi: Ppi) = toInch * ppi.value
	
	/**
	 * @return A modifier from this unit to millimeters
	 */
	def toMm = conversionModifierTo(MilliMeter)
	/**
	 * @return a modifier from this unit to centimeters
	 */
	def toCm = conversionModifierTo(CentiMeter)
	/**
	 * @return A modifier from this unit to meters
	 */
	def toM = conversionModifierTo(Meter)
	/**
	 * @return A modifier from this unit to kilometers
	 */
	def toKm = conversionModifierTo(KiloMeter)
	
	/**
	 * @return A modifier from this unit to inches
	 */
	def toInch = conversionModifierTo(Inch)
	/**
	 * @return A modifier from this unit to feet
	 */
	def toFeet = conversionModifierTo(Feet)
	
	
	// IMPLEMENTED  ---------------------
	
	override def toString = abbreviation
	
	
	// OTHER    -------------------------
	
	/**
	  * @param originUnit A unit
	  * @return Modifier that needs to be applied to a value in the specified unit
	  *         in order to receive a value in this unit
	  */
	def conversionModifierFrom(originUnit: DistanceUnit) = 1.0 / conversionModifierTo(originUnit)
	
	@deprecated("Please use .conversionModifierTo(DistanceUnit) or .conversionModifierFrom(DistanceUnit) instead", "1.5")
	def conversionModifierFor(targetUnit: DistanceUnit): Double = conversionModifierTo(targetUnit)
}

object DistanceUnit
{
	// TODO: Add a graph-based algorithm for unit conversions
	
	// NESTED   -------------------------
	
	object MeterUnit extends MetricUnitFactory[MeterUnit]
	{
		// ATTRIBUTES   -----------------
		
		private lazy val scaleMap = Map[MetricScale, MeterUnit](
			Default -> Meter,
			Deci -> DeciMeter,
			Centi -> CentiMeter,
			Milli -> MilliMeter,
			Micro -> MicroMeter,
			Kilo -> KiloMeter,
			Mega -> MegaMeter
		)
		
		
		// IMPLEMENTED  -----------------
		
		override def apply(scale: MetricScale): MeterUnit = scaleMap(scale)
	}
	
	sealed trait MeterUnit extends DistanceUnit with MetricUnit[MeterUnit]
	{
		// IMPLEMENTED  -----------------
		
		override def self: MeterUnit = this
		override def abbreviation: String = s"${scale.prefix}m"
		
		override protected def factory: MetricUnitFactory[MeterUnit] = MeterUnit
		
		
		// OTHER    ---------------------
		
		/**
		  * @param targetUnit Another unit
		  * @return Modifier that needs to be applied to this unit in order to produce a value in the specified unit
		  */
		protected def _conversionModifierTo(targetUnit: MeterUnit) = scale.modifierTo(targetUnit.scale)
	}
	
	
	// VALUES   -------------------------
	
	/**
	  * Micrometer length unit (1/1000/1000 of a meter)
	  */
	case object MicroMeter extends MeterUnit
	{
		override def scale: MetricScale = Micro
		
		override def conversionModifierTo(targetUnit: DistanceUnit): Double = targetUnit match {
			case MicroMeter => 1.0
			case u: MeterUnit => _conversionModifierTo(u)
			case _ => toCm * CentiMeter.conversionModifierTo(targetUnit)
		}
	}
	/**
	 * Millimeter length unit (1/1000 of a meter)
	 */
	case object MilliMeter extends MeterUnit
	{
		override def scale: MetricScale = Milli
		
		override def conversionModifierTo(targetUnit: DistanceUnit): Double = targetUnit match {
			case MilliMeter => 1.0
			case u: MeterUnit => _conversionModifierTo(u)
			case _ => toCm * CentiMeter.conversionModifierTo(targetUnit)
		}
	}
	/**
	 * Centimeter length unit (1/100 of a meter)
	 */
	case object CentiMeter extends MeterUnit
	{
		override def scale: MetricScale = Centi
		
		override def conversionModifierTo(targetUnit: DistanceUnit): Double = targetUnit match {
			case CentiMeter => 1.0
			case u: MeterUnit => _conversionModifierTo(u)
			case NauticalMile => toM * Meter.conversionModifierTo(KiloMeter)
			case Inch => 1 / 2.54
			case _ => toInch * Inch.conversionModifierTo(targetUnit)
		}
	}
	/**
	  * Decimeter length unit (1/10 of a meter)
	  */
	case object DeciMeter extends MeterUnit
	{
		override def scale: MetricScale = Deci
		
		override def conversionModifierTo(targetUnit: DistanceUnit): Double = targetUnit match {
			case DeciMeter => 1.0
			case u: MeterUnit => _conversionModifierTo(u)
			case _ => toCm * CentiMeter.conversionModifierTo(targetUnit)
		}
	}
	/**
	 * Meter length unit (standard metric length)
	 */
	case object Meter extends MeterUnit
	{
		override def scale: MetricScale = Default
		
		override def conversionModifierTo(targetUnit: DistanceUnit) = targetUnit match {
			case Meter => 1.0
			case u: MeterUnit => _conversionModifierTo(u)
			case Feet => 3.2808399
			case NauticalMile => toKm * KiloMeter.conversionModifierTo(NauticalMile)
			case _ => toFeet * Feet.conversionModifierTo(targetUnit)
		}
	}
	/**
	 * Kilometer length unit (1000 meters)
	 */
	case object KiloMeter extends MeterUnit
	{
		override def scale: MetricScale = Kilo
		
		override def conversionModifierTo(targetUnit: DistanceUnit) = targetUnit match {
			case KiloMeter => 1.0
			case u: MeterUnit => _conversionModifierTo(u)
			case Mile => 0.621371192
			case NauticalMile => 0.539956803
			case _ => toM * Meter.conversionModifierTo(targetUnit)
		}
	}
	/**
	  * Kilometer length unit (1 000 000 meters)
	  */
	case object MegaMeter extends MeterUnit
	{
		override def scale: MetricScale = Mega
		
		override def conversionModifierTo(targetUnit: DistanceUnit) = targetUnit match {
			case MegaMeter => 1.0
			case u: MeterUnit => _conversionModifierTo(u)
			case _ => toKm * KiloMeter.conversionModifierTo(targetUnit)
		}
	}
	
	/**
	 * Inch length unit
	 */
	case object Inch extends DistanceUnit
	{
		override val abbreviation: String = "in"
		
		override def conversionModifierTo(targetUnit: DistanceUnit) = targetUnit match {
			case Inch => 1.0
			case Feet => 0.0833333333
			case Mile => toFeet * Feet.conversionModifierTo(targetUnit)
			case CentiMeter => 2.54
			case _ => toCm * CentiMeter.conversionModifierTo(targetUnit)
		}
	}
	/**
	 * Feet length unit (standard imperial length unit)
	 */
	case object Feet extends DistanceUnit
	{
		override val abbreviation: String = "ft"
		
		override def conversionModifierTo(targetUnit: DistanceUnit) = targetUnit match {
			case Feet => 1.0
			case Inch => 12.0
			case Mile => 0.000189393939
			case Meter => 0.3048
			case _ => toM * Meter.conversionModifierTo(targetUnit)
		}
	}
	/**
	 * Mile length unit
	 */
	case object Mile extends DistanceUnit
	{
		override def abbreviation: String = "mi"
		
		override def conversionModifierTo(targetUnit: DistanceUnit) = targetUnit match {
			case Mile => 1.0
			case Feet => 5280.0
			case KiloMeter => 1.609344
			case NauticalMile => 0.868976242
			case _ => toFeet * Feet.conversionModifierTo(targetUnit)
		}
	}
	
	/**
	 * Distance unit used in sailing. Exactly 1 852 metres.
	 */
	case object NauticalMile extends DistanceUnit
	{
		override val abbreviation: String = "NM"
		
		override def conversionModifierTo(targetUnit: DistanceUnit) = targetUnit match {
			case NauticalMile => 1.0
			case Meter => 1852.0
			case KiloMeter => 1.852
			case Mile => 1.15077945
			case _ => toM * Meter.conversionModifierTo(targetUnit)
		}
	}
}
