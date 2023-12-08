package utopia.paradigm.measurement

import utopia.flow.operator.numeric.DoubleLike
import utopia.flow.operator.sign.{Sign, SignOrZero}
import utopia.paradigm.measurement.DistanceUnit.{CentiMeter, Inch}

object Ppi
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * A zero pixels per inch used when there is no screen or context available
	 */
	val zero = Ppi(0)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param pixels Amount of pixels that can be placed along 1 "perUnit"
	 * @param perUnit Unit to which the pixels are compared
	 * @return Amount of pixels per inch in that configuration
	 */
	def pixelsPer(pixels: Double, perUnit: DistanceUnit) = pixels / perUnit.conversionModifierTo(Inch)
	
	/**
	 * @param ppcm Pixels per centimeter
	 * @return Pixels per inch
	 */
	def pixelsPerCentimeter(ppcm: Double) = pixelsPer(ppcm, CentiMeter)
}

/**
 * Used for tracking the amount of pixels per inch
 * @author Mikko Hilpinen
 * @since Genesis 24.6.2020, v2.3
 */
case class Ppi(value: Double) extends DoubleLike[Ppi]
{
	// COMPUTED ---------------------------------
	
	/**
	 * @return The length of a single pixel in this context
	 */
	def pixelLength = Distance.ofInches(1 / value)
	
	
	// IMPLEMENTED  -----------------------------
	
	override def self = this
	
	override def sign: SignOrZero = Sign.of(value)
	override def length = value
	
	override def zero = Ppi.zero
	
	override def toString = s"${value.round} pixels per inch"
	
	override def +(other: Ppi) = Ppi(value + other.value)
	override def *(mod: Double) = Ppi(value * mod)
	
	override def compareTo(o: Ppi) =
	{
		val diff = value - o.value
		if (diff > 0)
			1
		else if (diff < 0)
			-1
		else
			0
	}
}
