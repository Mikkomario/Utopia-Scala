package utopia.genesis.util

import utopia.flow.util.RichComparable
import utopia.genesis.util.DistanceUnit.{CentiMeter, Inch}

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
	def pixelsPer(pixels: Double, perUnit: DistanceUnit) = pixels / perUnit.conversionModifierFor(Inch)
	
	/**
	 * @param ppcm Pixels per centimeter
	 * @return Pixels per inch
	 */
	def pixelsPerCentimeter(ppcm: Double) = pixelsPer(ppcm, CentiMeter)
}

/**
 * Used for tracking the amount of pixels per inch
 * @author Mikko Hilpinen
 * @since 24.6.2020, v2.3
 */
case class Ppi(value: Double) extends RichComparable[Ppi] with Scalable[Ppi]
{
	// COMPUTED ---------------------------------
	
	/**
	 * @return The length of a single pixel in this context
	 */
	def pixelLength = Distance.ofInches(1 / value)
	
	
	// IMPLEMENTED  -----------------------------
	
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
	
	override def toString = s"${value.round} pixels per inch"
	
	override def repr = this
	
	override def *(mod: Double) = Ppi(value * mod)
	
	
	// OTHER    ---------------------------------
	
	/**
	 * @param other Another Ppi
	 * @return Minimum between these amounts
	 */
	def min(other: Ppi) = RichComparable.min(this, other)
	
	/**
	 * @param other Another Ppi
	 * @return Maximum between these amounts
	 */
	def max(other: Ppi) = RichComparable.max(this, other)
}
