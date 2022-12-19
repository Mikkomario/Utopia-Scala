package utopia.paradigm.enumeration

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{Sign, SignedOrZero}
import utopia.paradigm.enumeration.LinearAlignment.Middle

/**
  * An enumeration for one-dimensional item aligning
  * @author Mikko Hilpinen
  * @since Genesis 29.1.2022, v2.6.3
  */
sealed trait LinearAlignment extends SignedOrZero[LinearAlignment]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Direction to which this alignment moves items. None if items should be kept at the middle / not moved.
	  */
	def direction: Option[Sign]
	
	/**
	  * @return The linear alignment opposite to this one
	  */
	def opposite: LinearAlignment
	
	/**
	  * Positions the starting point (left / top) of a length to fit within another length
	  * @param length Length of the item to position
	  * @param within Length of the area within which the item is positioned
	  * @return The starting position (top / left) of the 'length' so that it is positioned properly within the 'within'
	  */
	def position(length: Double, within: Double): Double
	
	/**
	  * Calculates the location matching this alignment on a one-dimensional line that starts from 0 and ends at
	  * the specified length.
	  * @param within Length of the target area
	  * @return Location matching this alignment at that length, which is:
	  *         Close => 0
	  *         Middle => within / 2
	  *         Far => within
	  */
	def origin(within: Double): Double
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Whether this alignment tends to move items from the middle to either direction
	  */
	def movesItems = direction.isDefined
	
	
	// IMPLEMENTED  --------------------------
	
	override def zero = Middle
	
	override def isPositive = direction.exists { _.isPositive }
	override def isZero = direction.isEmpty
	
	override def unary_- = opposite
	override def self = this
	
	
	// OTHER    ------------------------------
	
	/**
	  * Calculates the location matching this alignment within a one-dimensional span.
	  * @param within A span within which the location resides
	  * @return The location within the specified span that matches this alignment, namely:
	  *         Close => Start of the span,
	  *         Middle => Middle of the span,
	  *         Far => End of the span
	  */
	def origin(within: HasInclusiveEnds[Double]): Double = within.start + origin(within.end - within.start)
}

object LinearAlignment
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * All possible linear alignment values, from close to far
	  */
	val values = Vector[LinearAlignment](Close, Middle, Far)
	
	
	// NESTED   --------------------------
	
	/**
	  * An alignment which keeps items as close (small length) as possible
	  */
	case object Close extends LinearAlignment
	{
		override def direction = Some(Negative)
		
		override def opposite = Far
		
		override def position(length: Double, within: Double) = 0.0
		
		override def origin(within: Double) = 0.0
	}
	
	/**
	  * An alignment which keeps items as far (long length) as possible
	  */
	case object Far extends LinearAlignment
	{
		override def direction = Some(Positive)
		
		override def opposite = Close
		
		override def position(length: Double, within: Double) = within - length
		
		override def origin(within: Double) = within
	}
	
	/**
	  * An alignment which keeps items in the middle / doesn't move them to either direction
	  */
	case object Middle extends LinearAlignment
	{
		override def direction = None
		
		override def opposite = this
		
		override def position(length: Double, within: Double) = (within - length) / 2.0
		
		override def origin(within: Double) = within / 2.0
	}
}
