package utopia.vault.nosql.targeting

import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Mutate
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Column, Table}
import utopia.vault.model.template.Joinable
import utopia.vault.sql.{Condition, JoinType, OrderBy, SqlTarget}

/**
  * Common trait for instances which implement [[TargetingLike]] by wrapping another such instance
  * (while applying a mapping to its results).
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingWrapper[T <: TargetingLike[O, Val, T], O, +A, +Val, +Repr] extends TargetingLike[A, Val, Repr]
{
	// ABSTRACT ------------------------
	
	protected def wrapped: T
	
	protected def wrap(newTarget: T): Repr
	
	protected def wrapResult(result: O): A
	
	
	// IMPLEMENTED  --------------------
	
	override def target: SqlTarget = wrapped.target
	override def table: Table = wrapped.table
	
	override def accessCondition: Option[Condition] = wrapped.accessCondition
	
	override def pull(implicit connection: Connection): A = wrapResult(wrapped.pull)
	override def apply(column: Column, distinct: Boolean)(implicit connection: Connection): Val =
		wrapped(column, distinct)
	override def apply(columns: Seq[Column])(implicit connection: Connection): Seq[Val] = wrapped(columns)
	override def update(column: Column, value: Value)(implicit connection: Connection): Boolean =
		wrapped(column) = value
	
	override def apply(condition: Condition): Repr = mapWrapped { _(condition) }
	override def join(joins: Seq[Joinable], joinType: JoinType): Repr = mapWrapped { _.join(joins, joinType) }
	override def withOrdering(ordering: OrderBy): Repr = mapWrapped { _.withOrdering(ordering) }
	
	
	// OTHER    ------------------------
	
	protected def mapWrapped(f: Mutate[T]) = wrap(f(wrapped))
}
