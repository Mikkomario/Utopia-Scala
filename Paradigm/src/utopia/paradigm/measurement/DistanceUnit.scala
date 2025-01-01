package utopia.paradigm.measurement

import MetricScale._
import utopia.flow.collection.immutable.{Graph, Pair}

/**
 * An enumeration for standard distance units
 * @author Mikko Hilpinen
 * @since Genesis 24.6.2020, v2.3
 */
sealed abstract class DistanceUnit
{
	// IMPORTS  ------------------------------
	
	import DistanceUnit._
	
	
	// ATTRIBUTES   --------------------------
	
	private lazy val node = conversionGraph.node(this)
	
	
	// ABSTRACT ------------------------------
	
	/**
	  * @return The abbreviation used for this unit
	  */
	def abbreviation: String
	
	
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
	  * @return A modifier from this unit to millimeters
	  */
	def toMilliMeters = toMm
	/**
	 * @return a modifier from this unit to centimeters
	 */
	def toCm = conversionModifierTo(CentiMeter)
	/**
	  * @return a modifier from this unit to centimeters
	  */
	def toCentiMeters = toCm
	/**
	 * @return A modifier from this unit to meters
	 */
	def toM = conversionModifierTo(Meter)
	/**
	  * @return A modifier from this unit to meters
	  */
	def toMeters = toM
	/**
	 * @return A modifier from this unit to kilometers
	 */
	def toKm = conversionModifierTo(KiloMeter)
	/**
	  * @return A modifier from this unit to kilometers
	  */
	def toKiloMeters = toKm
	
	/**
	 * @return A modifier from this unit to inches
	 */
	def toInch = conversionModifierTo(Inch)
	/**
	 * @return A modifier from this unit to feet
	 */
	def toFeet = conversionModifierTo(Feet)
	
	/**
	  * @return A modifier from this unit to typographic points
	  */
	def toPoints = conversionModifierTo(Dtp)
	
	
	// IMPLEMENTED  ---------------------
	
	override def toString = abbreviation
	
	
	// OTHER    -------------------------
	
	/**
	  * @param amount Number of these units of distance
	  * @return A distance matching the specified amount of this unit
	  */
	def apply(amount: Double) = Distance(amount, this)
	
	/**
	  * @param targetUnit Another unit
	  * @return A modifier that must be applied to a number when converting from this unit to the other unit
	  */
	def conversionModifierTo(targetUnit: DistanceUnit): Double = {
		if (targetUnit == this)
			1.0
		else
			node.shortestRoutesToOne { _.value == targetUnit }.get.anyRoute.view.map { _.value }.product
	}
	
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
	// ATTRIBUTES   ---------------------
	
	/**
	  * All registered distance units
	  */
	val values = MeterUnit.values ++ Vector(Inch, Feet, Mile, NauticalMile, Dtp)
	
	/**
	  * A graph that may be utilized in unit conversions.
	  * Edges apply scaling modifiers assuming that the end node is the targeted (i.e. conversion result) unit.
	  */
	private val conversionGraph = Graph[DistanceUnit, Double](Set(
		(MegaMeter, KiloMeter, 1000.0),
		(MegaMeter, Meter, 1000000.0),
		(KiloMeter, Meter, 1000.0),
		(Meter, DeciMeter, 10.0),
		(Meter, CentiMeter, 100.0),
		(Meter, MilliMeter, 1000.0),
		(Meter, Feet, 3.2808399),
		(DeciMeter, CentiMeter, 10.0),
		(DeciMeter, MilliMeter, 100.0),
		(CentiMeter, MilliMeter, 10.0),
		(MilliMeter, MicroMeter, 1000.0),
		(NauticalMile, Mile, 1.15077945),
		(NauticalMile, KiloMeter, 1.852),
		(NauticalMile, Meter, 1852.0),
		(Mile, Feet, 5280.0),
		(Mile, KiloMeter, 1.609344),
		(Feet, Inch, 12.0),
		(Inch, CentiMeter, 2.54),
		(Inch, Dtp, 72.0),
		(Dtp, MilliMeter, 0.3528)
	).flatMap { case (from, to, mod) => Pair((from, mod, to), (to, 1.0 / mod, from)) })
	
	
	// NESTED   -------------------------
	
	object MeterUnit extends MetricUnitFactory[MeterUnit]
	{
		// ATTRIBUTES   -----------------
		
		/**
		  * All registered meter unit values
		  */
		val values = Vector[MeterUnit](MicroMeter, MilliMeter, CentiMeter, DeciMeter, Meter, KiloMeter, MegaMeter)
		
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
	sealed abstract class MeterUnit extends DistanceUnit with MetricUnit[MeterUnit]
	{
		// ATTRIBUTES   -----------------
		
		override protected val factory: MetricUnitFactory[MeterUnit] = MeterUnit
		
		override lazy val abbreviation: String = s"${scale.prefix}m"
		
		
		// IMPLEMENTED  -----------------
		
		override def self: MeterUnit = this
		
		
		// OTHER    ---------------------
		
		override def conversionModifierTo(targetUnit: DistanceUnit) = targetUnit match {
			case metric: MeterUnit => scale.modifierTo(metric.scale)
			case u => super.conversionModifierTo(u)
		}
	}
	
	
	// VALUES   -------------------------
	
	/**
	  * Micrometer length unit (1/1000/1000 of a meter)
	  */
	case object MicroMeter extends MeterUnit
	{
		override val scale: MetricScale = Micro
	}
	/**
	 * Millimeter length unit (1/1000 of a meter)
	 */
	case object MilliMeter extends MeterUnit
	{
		override val scale: MetricScale = Milli
	}
	/**
	 * Centimeter length unit (1/100 of a meter)
	 */
	case object CentiMeter extends MeterUnit
	{
		override val scale: MetricScale = Centi
	}
	/**
	  * Decimeter length unit (1/10 of a meter)
	  */
	case object DeciMeter extends MeterUnit
	{
		override val scale: MetricScale = Deci
	}
	/**
	 * Meter length unit (standard metric length)
	 */
	case object Meter extends MeterUnit
	{
		override val scale: MetricScale = Default
	}
	/**
	 * Kilometer length unit (1000 meters)
	 */
	case object KiloMeter extends MeterUnit
	{
		override val scale: MetricScale = Kilo
	}
	/**
	  * Kilometer length unit (1 000 000 meters)
	  */
	case object MegaMeter extends MeterUnit
	{
		override val scale: MetricScale = Mega
	}
	
	/**
	 * Inch length unit
	 */
	case object Inch extends DistanceUnit
	{
		override val abbreviation: String = "in"
	}
	/**
	 * Feet length unit (standard imperial length unit)
	 */
	case object Feet extends DistanceUnit
	{
		override val abbreviation: String = "ft"
	}
	/**
	 * Mile length unit
	 */
	case object Mile extends DistanceUnit
	{
		override val abbreviation: String = "mi"
	}
	
	/**
	 * Distance unit used in sailing. Exactly 1 852 metres.
	 */
	case object NauticalMile extends DistanceUnit
	{
		override val abbreviation: String = "NM"
	}
	
	/**
	  * Distance unit used in typography, known as "point".
	  * One point is 1/72 inches, or 0.3528 millimeters.
	  * This distance is often used with fonts and related measurements.
	  */
	case object Dtp extends DistanceUnit
	{
		override val abbreviation: String = "pt"
	}
}
