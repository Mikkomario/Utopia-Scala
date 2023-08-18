package utopia.paradigm.enumeration

import utopia.flow.collection.immutable.range.{HasInclusiveEnds, NumericSpan}
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.SignOrZero.Neutral
import utopia.flow.operator.{SignOrZero, SignedOrZero}
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
	  * @return Direction to which this alignment moves items.
	  */
	def direction: SignOrZero
	
	/**
	  * @return The linear alignment opposite to this one
	  */
	def opposite: LinearAlignment
	
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
	/**
	  * Positions the starting point (left / top) of a length to fit within another length.
	  *
	  * If the specified area 'length' is larger than the specified target area 'within', the starting point will be
	  * located so that the 'within' area is fully covered and the remaining area will be on the side that matches
	  * this alignment.
	  *
	  * E.g. For Close, the default is to start at 0, but for larger areas, will start at negative coordinates
	  * (i.e. more "close")
	  *
	  * @param length Length of the item to position
	  * @param within Length of the area within which the item is positioned
	  *
	  * @return The starting position (top / left) of the 'length' so that it is positioned properly within the 'within'
	  */
	def position(length: Double, within: Double): Double
	/**
	  * Positions the specified area relative to another area so that their relationship matches this alignment.
	  * E.g. When called for Close, places the resulting area before the 'to' area.
	  * Far places after and Middle places over.
	  * @param area   An area (length) to position
	  * @param to     The area close to which the 'area' will be placed
	  * @param margin Margin placed between the two areas. Default = 0.
	  *               Will not be taken into account when called for Middle.
	  * @return An area that's placed relative to the other area
	  */
	def positionRelativeTo(area: Double, to: HasInclusiveEnds[Double], margin: Double = 0.0): NumericSpan[Double]
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Whether this alignment tends to move items from the middle to either direction
	  */
	def movesItems = direction.isNotNeutral
	
	
	// IMPLEMENTED  --------------------------
	
	override def sign: SignOrZero = direction
	
	override def self = this
	override def zero = Middle
	override def unary_- = opposite
	
	
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
	
	/**
	  * Positions a linear area over another linear area so that this alignment is followed.
	  *
	  * E.g. When using Close alignment, 'area' will be placed to start at the beginning of 'within'
	  * (unless it is larger, in which case it expands towards the negative (i.e. close) direction)
	  *
	  * @param area An area to position
	  * @param within An area within which the specified area is moved to
	  * @return A new area that resides within the specified area (or fully contains it),
	  *         and is positioned according to this linear alignment
	  *         (i.e. to the beginning, to the middle or to the end of the specified area)
	  */
	def position(area: Double, within: HasInclusiveEnds[Double]): NumericSpan[Double] = {
		val relative = position(area, within.end - within.start)
		val start = within.start + relative
		NumericSpan(start, start + area)
	}
	/**
	  * Positions a linear area within another area, based on this alignment.
	  * The resulting area will always fit within the 'within' area and may be smaller than the specified 'area'.
	  * @param area Area to position
	  * @param within Area within the 'area' must fit
	  * @return An area that fits within the 'within' area and is positioned according to this alignment
	  */
	def fit(area: Double, within: HasInclusiveEnds[Double]) = {
		val withinLength = within.end - within.start
		// Case: Area is too long and will be squeezed
		if (area >= withinLength)
			NumericSpan.from(within)
		// Case: Area fits to 'within'
		else {
			val relative = position(area, withinLength)
			val start = within.start + relative
			NumericSpan(start, start + area)
		}
	}
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
		override def direction = Negative
		override def opposite = Far
		
		override def origin(within: Double) = 0.0
		override def position(length: Double, within: Double) = {
			val excess = length - within
			-(excess max 0)
		}
		override def positionRelativeTo(area: Double, to: HasInclusiveEnds[Double], margin: Double) = {
			val end = to.start - margin
			NumericSpan(end - area, end)
		}
	}
	/**
	  * An alignment which keeps items as far (long length) as possible
	  */
	case object Far extends LinearAlignment
	{
		override def direction = Positive
		override def opposite = Close
		
		override def origin(within: Double) = within
		override def position(length: Double, within: Double) = within - (length min within)
		override def positionRelativeTo(area: Double, to: HasInclusiveEnds[Double], margin: Double) = {
			val start = to.end + margin
			NumericSpan(start, start + area)
		}
	}
	/**
	  * An alignment which keeps items in the middle / doesn't move them to either direction
	  */
	case object Middle extends LinearAlignment
	{
		override def direction = Neutral
		override def opposite = this
		
		override def origin(within: Double) = within / 2.0
		override def position(length: Double, within: Double) = (within - length) / 2.0
		override def positionRelativeTo(area: Double, to: HasInclusiveEnds[Double], margin: Double) =
			position(area, to)
	}
}
