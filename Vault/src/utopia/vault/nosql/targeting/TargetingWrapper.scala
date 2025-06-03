package utopia.vault.nosql.targeting

import utopia.flow.util.Mutate
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Table
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.targeting.columns.AccessColumnsWrapper
import utopia.vault.sql.{Condition, JoinType, OrderBy, SqlTarget}

/**
  * Common trait for instances which implement [[TargetingLike]] by wrapping another such instance
  * (while applying a mapping to its results).
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingWrapper[T <: TargetingLike[O, OV, T], O, OV, +R, +RV, +Repr]
	extends TargetingLike[R, RV, Repr] with AccessColumnsWrapper[OV, RV]
{
	// ABSTRACT ------------------------
	
	protected def wrapped: T
	
	protected def wrap(newTarget: T): Repr
	
	protected def wrapResult(result: O): R
	
	
	// IMPLEMENTED  --------------------
	
	override def target: SqlTarget = wrapped.target
	override def table: Table = wrapped.table
	
	override def accessCondition: Option[Condition] = wrapped.accessCondition
	
	override def pull(implicit connection: Connection): R = wrapResult(wrapped.pull)
	
	override def apply(condition: Condition): Repr = mapWrapped { _(condition) }
	override def join(joins: Seq[Joinable], joinType: JoinType): Repr = mapWrapped { _.join(joins, joinType) }
	override def withOrdering(ordering: OrderBy): Repr = mapWrapped { _.withOrdering(ordering) }
	
	
	// OTHER    ------------------------
	
	protected def mapWrapped(f: Mutate[T]) = {
		// Won't construct a new wrapper if the wrapped access won't change
		val original = wrapped
		val mapped = f(wrapped)
		if (original == mapped)
			self
		else
			wrap(mapped)
	}
}
