package utopia.vault.nosql.targeting.many

import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.TargetingLike

/**
  * Common trait for access points that yield multiple items at a time
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingManyLike[+A, +Repr] extends TargetingLike[Seq[A], Seq[Value], Repr]
{
	// OTHER    ------------------------
	
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
