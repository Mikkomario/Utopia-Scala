package utopia.vault.nosql.targeting.many

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.enumeration.{End, Extreme}
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.TargetingLike
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.view.ViewManyByIntIds
import utopia.vault.sql.{Condition, OrderBy}

/**
  * Common trait for access points that yield multiple items at a time
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingManyLike[+A, +Repr, +One]
	extends TargetingLike[Seq[A], Seq[Value], Repr] with AccessManyColumns with ViewManyByIntIds[Repr]
{
	// ABSTRACT ------------------------
	
	/**
	  * @param end The targeted end
	  * @param order End-determining order (optional). Overrides currently applied ordering, if specified.
	  * @param filter An additional filtering-condition to apply (optional)
	  * @return Access to the item at the specified end of this access' range
	  */
	def apply(end: End, order: Option[OrderBy] = None, filter: Option[Condition] = None): One
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Access to the first targeted entry
	 */
	def head = apply(First)
	/**
	 * @return Access to the last targeted entry
	 */
	def last = apply(Last)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param id Targeted index
	  * @return Access to the targeted row / item, provided it's accessible by this interface
	  */
	def apply(id: Int) = find(index <=> id)
	
	/**
	 * @param ordering Applied ordering
	 * @return Access to the largest / last item when using the specified ordering
	 */
	def maxBy(ordering: OrderBy) = apply(Max, ordering)
	/**
	 * @param column An ordering column
	 * @return Access to the largest item, according to its column value
	 */
	def maxBy(column: Column): One = maxBy(OrderBy.ascending(column))
	/**
	 * @param ordering Applied ordering
	 * @return Access to the smallest / first item when using the specified ordering
	 */
	def minBy(ordering: OrderBy) = apply(Min, ordering)
	/**
	 * @param column An ordering column
	 * @return Access to the smallest item, according to its column value
	 */
	def minBy(column: Column): One = minBy(OrderBy.ascending(column))
	
	/**
	  * @param end Targeted end
	  * @param by Applied ordering
	  * @return Access to the first or the last item when applying the specified ordering
	  */
	def apply(end: End, by: OrderBy): One = apply(end, Some(by))
	/**
	 * @param extreme Targeted extreme
	 * @param by Applied reference column
	 * @return Access to the most extreme item when ordering by the specified column
	 */
	def apply(extreme: Extreme, by: Column): One = apply(extreme, OrderBy.ascending(by))
	/**
	  * @param extreme Targeted extreme
	  * @param by Applied reference column
	  * @return Access to the most extreme item when applying the specified ordering
	  */
	def apply(extreme: Extreme, by: OrderBy): One = apply(extreme.toEnd, by)
	
	/**
	  * @param condition Applied search condition
	  * @return Access to the first accessible item that satisfied the specified condition
	  */
	def find(condition: Condition) = findEnd(First, condition)
	/**
	  * @param condition Applied search condition
	  * @return Access to the last accessible item that satisfied the specified condition,
	  *         using natural / default ordering
	  */
	def findLast(condition: Condition) = findEnd(Last, condition)
	/**
	  * @param end Targeted end
	  * @param condition Applied search condition
	  * @return Access to the first or the last accessible item that satisfied the specified condition,
	  *         using natural / default ordering
	  */
	def findEnd(end: End, condition: Condition): One = apply(end, filter = Some(condition))
	
	/**
	  * @param f A mapping function
	  * @param c Implicit DB Connection
	  * @tparam B Type of map results
	  * @return A map where each accessible item is mapped to a map result
	  */
	def toMapBy[B](f: A => B)(implicit c: Connection) = pull.iterator.map { a => f(a) -> a }.toMap
}
