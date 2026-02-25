package utopia.echo.model.enumeration

import utopia.echo.model.enumeration.ByteCountUnit.{GigaBytes, MegaBytes, TeraBytes}
import utopia.flow.operator.ordering.SelfComparable

/**
 * An enumeration for different byte count units (MB, GB, etc.)
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
sealed trait ByteCountUnit extends SelfComparable[ByteCountUnit]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Multiplier applied to values of this unit in order to get MB
	 */
	def toMegaMultiplier: Int
	/**
	 * @return Multiplier applied to values of this unit in order to get GB
	 */
	def toGigaMultiplier: Double
	
	
	// IMPLEMENTED  ---------------------
	
	override def self: ByteCountUnit = this
	
	// Smaller multiplier => Larger unit
	override def compareTo(o: ByteCountUnit): Int = o.toMegaMultiplier.compareTo(toMegaMultiplier)
	
	
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
				case MegaBytes => toMegaMultiplier
				case GigaBytes => toGigaMultiplier
				case TeraBytes => toGigaMultiplier / 1000
			}
	}
}

object ByteCountUnit
{
	// VALUES   -------------------------
	
	case object MegaBytes extends ByteCountUnit
	{
		override val toMegaMultiplier = 1
		override val toGigaMultiplier: Double = 0.001
	}
	
	case object GigaBytes extends ByteCountUnit
	{
		override val toMegaMultiplier = 1000
		override val toGigaMultiplier: Double = 1
	}
	
	case object TeraBytes extends ByteCountUnit
	{
		override val toMegaMultiplier = 1000000
		override val toGigaMultiplier: Double = 1000
	}
}
