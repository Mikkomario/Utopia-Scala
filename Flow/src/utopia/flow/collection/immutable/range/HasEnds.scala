package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair

object HasEnds
{
	// OTHER    -------------------------
	
	/**
	  * @param start The starting point to use
	  * @param end The ending point to use
	  * @param isInclusive Whether the specified ending point is inclusive (true) or exclusive (false)
	  * @tparam P Type of range endpoints
	  * @return A new range
	  */
	def apply[P](start: P, end: P, isInclusive: Boolean): HasEnds[P] = _HasEnds(start, end, isInclusive)
	/**
	  * @param ends The start and end points to use
	  * @param isInclusive Whether the specified ending point is inclusive (true) or exclusive (false)
	  * @tparam P Type of range endpoints
	  * @return A new range
	  */
	def apply[P](ends: Pair[P], isInclusive: Boolean): HasEnds[P] = PairHasEnds(ends, isInclusive)
	
	/**
	  * @param start       The starting point to use
	  * @param end         The ending point to use
	  * @tparam P Type of range endpoints
	  * @return A new range
	  */
	def inclusive[P](start: P, end: P) = HasInclusiveEnds(start, end)
	/**
	  * @param ends        The start and end points to use
	  * @tparam P Type of range endpoints
	  * @return A new range
	  */
	def inclusive[P](ends: Pair[P]) = HasInclusiveEnds(ends)
	
	/**
	  * @param start       The starting point to use
	  * @param end         The ending point to use (exclusive)
	  * @tparam P Type of range endpoints
	  * @return A new range
	  */
	def exclusive[P](start: P, end: P) = apply(start, end, isInclusive = false)
	/**
	  * @param ends The start and end points to use, where the second point is exclusive
	  * @tparam P Type of range endpoints
	  * @return A new range
	  */
	def exclusive[P](ends: Pair[P]) = apply(ends, isInclusive = false)
	
	
	// EXTENSIONS   -------------------
	
	implicit class HasIntegerEnds(val range: HasEnds[Int]) extends AnyVal
	{
		/**
		 * @throws UnsupportedOperationException If this range is empty
		 * @return Inclusive ends of this range
		 */
		@throws[UnsupportedOperationException]("If this range is empty")
		def inclusiveEnds = inclusiveEndsOption.getOrElse {
			throw new UnsupportedOperationException("An empty range has no inclusive ends")
		}
		/**
		 * @return Inclusive versions of the ends of this range. None if this range is empty.
		 */
		def inclusiveEndsOption = {
			// Case: Already inclusive => Just returns the ends
			if (range.isInclusive)
				Some(range.ends)
			// Case: Empty range => Throws
			else if (range.isEmpty)
				None
			// Case: Exclusive non-empty range => Modifies the end, so that it's inclusive
			else {
				val ends = range.ends
				if (ends.first < ends.second)
					Some(ends.mapSecond { _ - 1 })
				else
					Some(ends.mapSecond { _ + 1 })
			}
		}
	}
	
	
	// NESTED   -------------------------
	
	private case class _HasEnds[+P](start: P, end: P, isInclusive: Boolean) extends HasEnds[P]
	{
		override lazy val ends = super.ends
	}
	
	private case class PairHasEnds[+P](override val ends: Pair[P], isInclusive: Boolean) extends HasEnds[P]
	{
		override def start: P = ends.first
		override def end: P = ends.second
	}
}

/**
  * A common trait for items which have two ends: A start and an end.
  * The end-point may be inclusive or exclusive.
  * @tparam P Type of end-points on this item
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait HasEnds[+P] extends MayHaveEnds[P]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The starting point of this range
	  */
	def start: P
	/**
	  * @return The ending point of this range
	  * @see [[isInclusive]]
	  */
	def end: P
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return A pair containing the start and end points of this range
	  */
	@deprecated("Please use .ends instead", "v2.2")
	def toPair = ends
	
	
	// IMPLEMENTED  ---------------------
	
	/**
	 * @return Whether this is an empty range
	 */
	override def isEmpty = isExclusive && start == end
	
	override def startOption: Option[P] = Some(start)
	override def endOption: Option[P] = Some(end)
	
	/**
	 * @return A pair containing the start and end points of this range
	 */
	override def ends = Pair(start, end)
}
