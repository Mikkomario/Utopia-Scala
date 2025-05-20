package utopia.vault.nosql.targeting.many

import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.enumeration.{End, Extreme}
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.TargetingLike
import utopia.vault.sql.OrderBy

/**
  * Common trait for access points that yield multiple items at a time
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingManyLike[+A, +Repr, +One] extends TargetingLike[Seq[A], Seq[Value], Repr]
{
	// ABSTRACT ------------------------
	
	/**
	  * @param end The targeted end
	  * @param ordering End-determining order (optional). Overrides currently applied ordering, if specified.
	  * @return Access to the item at the specified end of this access' range
	  */
	def apply(end: End, ordering: Option[OrderBy] = None): One
	
	
	// COMPUTED ------------------------
	
	def head = apply(First)
	def last = apply(Last)
	
	
	// OTHER    ------------------------
	
	def maxBy(ordering: OrderBy) = apply(Max, ordering)
	def minBy(ordering: OrderBy) = apply(Min, ordering)
	
	def apply(extreme: Extreme, by: OrderBy): One = apply(extreme.toEnd, Some(by))
	
	/**
	  * @param f A mapping function
	  * @param c Implicit DB Connection
	  * @tparam B Type of map results
	  * @return A map where each accessible item is mapped to a map result
	  */
	def toMapBy[B](f: A => B)(implicit c: Connection) = pull.iterator.map { a => f(a) -> a }.toMap
	
	def toMap[K, V](key: Column, value: Column)(makeKey: Value => K)(makeValue: Value => V)
	               (implicit connection: Connection) =
		apply(key, value).iterator
			.map { values =>
				val iter = values.iterator
				makeKey(iter.nextOption().getOrElse(Value.empty)) -> makeValue(iter.nextOption().getOrElse(Value.empty))
			}
			.toMap
	def toMultiMap[K, V](key: Column, value: Column)(makeKey: Value => K)(makeValues: Seq[Value] => V)
	                    (implicit connection: Connection) =
		apply(key, value)
			.map { values =>
				val iter = values.iterator
				makeKey(iter.nextOption().getOrElse(Value.empty)) -> iter.nextOption().getOrElse(Value.empty)
			}
			.groupMap { _._1 } { _._2 }
			.view.mapValues(makeValues).toMap
}
