package utopia.echo.model.enumeration

import utopia.echo.model.enumeration.ByteCountUnit.{GibiBytes, MebiBytes, TebiBytes}
import utopia.flow.operator.ordering.SelfComparable

/**
 * An enumeration for different byte count units (MiB, GiB, etc.)
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
sealed trait ByteCountUnit extends SelfComparable[ByteCountUnit]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Code representing this unit. E.g. "GiB"
	 */
	def code: String
	
	/**
	 * @return Multiplier applied to values of this unit in order to get MiB
	 */
	def toMegaMultiplier: Int
	/**
	 * @return Multiplier applied to values of this unit in order to get GiB
	 */
	def toGigaMultiplier: Double
	
	
	// IMPLEMENTED  ---------------------
	
	override def self: ByteCountUnit = this
	
	override def toString: String = code
	
	// Larger multiplier => Larger unit
	override def compareTo(o: ByteCountUnit): Int = toMegaMultiplier.compareTo(o.toMegaMultiplier)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param targetUnit Targeted unit
	 * @return A multiplier to apply to a byte count value in order to acquire values in this unit
	 */
	def multiplierTo(targetUnit: ByteCountUnit) = {
		if (targetUnit == this)
			1
		else
			targetUnit match {
				case MebiBytes => toMegaMultiplier
				case GibiBytes => toGigaMultiplier
				case TebiBytes => toGigaMultiplier / 1024
			}
	}
}

object ByteCountUnit
{
	// VALUES   -------------------------
	
	case object MebiBytes extends ByteCountUnit
	{
		override val code: String = "MiB"
		override val toMegaMultiplier = 1
		override val toGigaMultiplier: Double = 0.0009765625
	}
	
	case object GibiBytes extends ByteCountUnit
	{
		override val code: String = "GiB"
		override val toMegaMultiplier = 1024
		override val toGigaMultiplier: Double = 1
	}
	
	case object TebiBytes extends ByteCountUnit
	{
		override val code: String = "TiB"
		override val toMegaMultiplier = 1024 * 1024
		override val toGigaMultiplier: Double = 1024
	}
}
