package utopia.vault.nosql.targeting

import utopia.flow.collection.immutable.Single
import utopia.vault.database.Connection
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.targeting.columns.AccessColumns
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{JoinType, OrderBy}

/**
  * Common trait for access points that may be filtered and/or extended
  * @tparam Repr Type of filtered / further targeted access points generated
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingLike[+A, +V, +Repr] extends AccessColumns[V] with FilterableView[Repr]
{
	// ABSTRACT ----------------------------
	
	/**
	  * Pulls the targeted data
	  * @param connection Implicit DB connection
	  * @return Targeted data
	  */
	def pull(implicit connection: Connection): A
	
	/**
	  * @param joins Additional joins to apply
	  * @param joinType Type of joins applied
	  * @return Copy of this access point including the specified joins
	  */
	def join(joins: Seq[Joinable], joinType: JoinType = Inner): Repr
	
	/**
	  * @param ordering Ordering to apply
	  * @return A copy of this access point with the specified ordering applied
	  */
	def withOrdering(ordering: OrderBy): Repr
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param join First join to apply
	  * @param more More joins to apply
	  * @return A copy of this access which applies the specified joins
	  */
	def join(join: Joinable, more: Joinable*): Repr = this.join(Single(join) ++ more)
	/**
	 * @param join First join to apply
	 * @param more More joins to apply
	 * @return A copy of this access which applies the specified joins as left joins
	 */
	def leftJoin(join: Joinable, more: Joinable*): Repr = this.join(Single(join) ++ more, JoinType.Left)
}
