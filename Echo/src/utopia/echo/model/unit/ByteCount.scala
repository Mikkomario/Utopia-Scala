package utopia.echo.model.unit

import utopia.echo.model.enumeration.ByteCountUnit
import utopia.echo.model.enumeration.ByteCountUnit.MegaBytes
import utopia.flow.operator.Reversible
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.LinearScalable

/**
 * Used for measuring volume or traffic in bytes
 * @param amount Amount on bytes in 'unit'
 * @param unit Unit in which 'amount' is given
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
case class ByteCount(amount: Int, unit: ByteCountUnit)
	extends SelfCombinable[ByteCount] with Reversible[ByteCount] with LinearScalable[ByteCount]
{
	// COMPUTED --------------------
	
	/**
	 * @return This amount in MB
	 */
	def megas = amount * unit.toMegaMultiplier
	/**
	 * @return This amount in GB
	 */
	def gigas = amount * unit.toGigaMultiplier
	
	
	// IMPLEMENTED  ---------------
	
	override def self: ByteCount = this
	override def unary_- : ByteCount = copy(amount = -amount)
	
	override def +(other: ByteCount): ByteCount = {
		val appliedUnit = unit min other.unit
		ByteCount((to(appliedUnit) + other.to(appliedUnit)).round.toInt, appliedUnit)
	}
	
	override def *(mod: Double): ByteCount = ByteCount((megas * mod).round.toInt, MegaBytes)
	
	
	// OTHER    -------------------
	
	def *(mod: Int) = copy(amount = amount * mod)
	
	/**
	 * @param other Another byte count
	 * @return Ratio between these two amounts
	 */
	def /(other: ByteCount) = {
		val compareUnit = unit min other.unit
		to(compareUnit) / other.to(compareUnit)
	}
	
	/**
	 * @param unit Targeted unit
	 * @return This amount in that unit
	 */
	def to(unit: ByteCountUnit) = {
		if (unit == this.unit)
			amount
		else
			amount * this.unit.multiplierTo(unit)
	}
}