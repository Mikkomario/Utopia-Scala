package utopia.vault.nosql.targeting.grouped

import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.enumeration.Extreme
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.TargetingWrapper
import utopia.vault.sql.OrderDirection

/**
  * Common trait for access points that target multiple rows at a time by wrapping another targeted access point
  * @tparam T Type of the wrapped access point (must target multiple rows)
 * @tparam O Type of the items / results pulled using the wrapped access point
 * @tparam A Type of the wrapped / mapped results (O => A)
 * @tparam Repr Concrete / implementing type of this access point, yielded by various copy functions
 * @author Mikko Hilpinen
  * @since 21.01.2026, v2.1
  */
trait TargetingGroupedWrapper[T <: TargetingGroupedLike[O, T], O, +A, +Repr]
	extends TargetingGroupedLike[A, Repr]
		with TargetingWrapper[T, O, Seq[Value], Seq[Seq[Value]], A, Seq[Value], Seq[Seq[Value]], Repr]
{
	// IMPLEMENTED  -----------------------
	
	override protected def wrapValue(value: Seq[Value]): Seq[Value] = value
	override protected def wrapValues(values: Seq[Seq[Value]]): Seq[Seq[Value]] = values
	
	override def apply(column: Column, extreme: Extreme)(implicit connection: Connection): Value =
		wrapped(column, extreme)
	
	override def count(column: Column, distinct: Boolean)(implicit connection: Connection) =
		wrapped.count(column, distinct)
	
	override def streamColumn[B](column: Column, order: Option[OrderDirection], distinct: Boolean)
	                            (f: Iterator[Value] => B)
	                            (implicit connection: Connection) =
		wrapped.streamColumn(column, order, distinct)(f)
	override def streamColumns[B](columns: Seq[Column])(f: Iterator[Seq[Value]] => B)
	                             (implicit connection: Connection) =
		wrapped.streamColumns(columns)(f)
}
