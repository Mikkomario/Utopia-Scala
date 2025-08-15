package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair

/**
  * A common trait for items which may have one or two ends.
  * The end-point may be inclusive or exclusive.
  * @tparam P Type of end-points on this item
  * @author Mikko Hilpinen
  * @since 15.8.2025, v2.7
  */
trait MayHaveEnds[+P]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The starting point of this range, if applicable
	  */
	def startOption: Option[P]
	/**
	  * @return The ending point of this range, if applicable
	  * @see [[isInclusive]]
	  */
	def endOption: Option[P]
	
	/**
	 * @return Whether this is an empty range (which contains no values)
	 */
	def isEmpty: Boolean
	/**
	  * @return Whether the end-point of this range is inclusive (true) or exclusive (false).
	  *         If the end is not specifically defined, yields true if this is a non-empty range.
	  */
	def isInclusive: Boolean
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return Whether this range has a specified starting point
	 */
	def hasStart = startOption.isDefined
	/**
	 * @return Whether this range has a specified ending point
	 */
	def hasEnd = endOption.isDefined
	
	/**
	  * @return Whether this is an exclusive range.
	  *         The 'end' in exclusive ranges is not considered to be contained within the range itself.
	  */
	def isExclusive = !isInclusive
	
	/**
	  * @return The known end points of this range (0-2)
	  */
	def ends: Seq[P] = Pair(startOption, endOption).flatten
	
	/**
	  * @return Whether this range is not empty
	  */
	def nonEmpty = !isEmpty
	
	
	// IMPLEMENTED  ---------------------
	
	override def toString = startOption match {
		case Some(start) =>
			endOption match {
				case Some(end) =>
					if (isInclusive) {
						if (start == end)
							start.toString
						else
							s"$start to $end"
					}
					else
						s"$start until $end"
					
				case None => "from $start"
			}
		case None =>
			endOption match {
				case Some(end) =>
					if (isInclusive)
						s"to $end"
					else
						s"until $end"
					
				case None =>
					if (isEmpty)
						"none"
					else
						"all"
			}
	}
}
