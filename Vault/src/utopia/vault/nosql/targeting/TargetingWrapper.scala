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
 * @tparam T Type of the wrapped access point
 * @tparam O Type of the .pull results in the wrapped access point (T)
 * @tparam OV Type of values or value-sets pulled using the wrapped access point (T),
 *            when targeting individual columns.
 *            I.e. V of T.
 * @tparam OVV Type of value-sets pulled using the wrapped access point, when targeting multiple columns at once.
 *             I.e. VV of T.
 * @tparam R The type of pulled and wrapped items (as a group) (i.e. one or multiple items of some kind)
 * @tparam RV Type of values or value-sets pulled when targeting individual columns, after wrapping
 * @tparam RVV Type of value-sets / rows pulled when targeting multiple columns at once, after wrapping
 * @tparam Repr Type of filtered / further targeted access points generated
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingWrapper[T <: TargetingLike[O, OV, OVV, T], O, OV, OVV, +R, +RV, +RVV, +Repr]
	extends TargetingLike[R, RV, RVV, Repr] with AccessColumnsWrapper[OV, OVV, RV, RVV]
{
	// ABSTRACT ------------------------
	
	protected def wrapped: T
	
	/**
	  * @param newTarget New targeting access to wrap
	  * @return A new wrapper
	  */
	protected def wrap(newTarget: T): Repr
	/**
	  * @param result Result acquired from the wrapped access point
	  * @return Wrapped results
	  */
	protected def wrapResult(result: O): R
	
	
	// IMPLEMENTED  --------------------
	
	override def target: SqlTarget = wrapped.target
	override def table: Table = wrapped.table
	
	override def accessCondition: Option[Condition] = wrapped.accessCondition
	
	override def pull(implicit connection: Connection): R = wrapResult(wrapped.pull)
	
	override def apply(condition: Condition): Repr = mapWrapped { _(condition) }
	override def join(joins: Seq[Joinable], joinType: JoinType): Repr = mapWrapped { _.join(joins, joinType) }
	override def notLinkedTo(table: Table, where: Option[Condition]) = mapWrapped { _.notLinkedTo(table, where) }
	override def withOrdering(ordering: OrderBy): Repr = mapWrapped { _.withOrdering(ordering) }
	
	
	// OTHER    ------------------------
	
	/**
	  * @param f A mapping function applied to the wrapped access point
	  * @return A wrapper wrapping the mapped access point
	  */
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
