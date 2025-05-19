package utopia.vault.nosql.targeting

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{JoinType, OrderBy}

/**
  * Common trait for access points that may be filtered and/or extended
  * @tparam Repr Type of filtered / further targeted access points generated
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingLike[+A, +Val, +Repr] extends FilterableView[Repr]
{
	// ABSTRACT ----------------------------
	
	/**
	  * Pulls the targeted data
	  * @param connection Implicit DB connection
	  * @return Targeted data
	  */
	def pull(implicit connection: Connection): A
	/**
	  * @param column Targeted column
	  * @param distinct Whether the targeted column values should be distinct from each other (default = false)
	  * @return Data from the targeted column within the targeted data
	  */
	def apply(column: Column, distinct: Boolean)(implicit connection: Connection): Val
	/**
	  * @param columns Targeted columns
	  * @param connection Implicit DB connection
	  * @return Data of the targeted columns from the targeted item(s)
	  */
	def apply(columns: Seq[Column])(implicit connection: Connection): Seq[Val]
	/**
	  * Updates the column value(s) of the targeted item(s)
	  * @param column Targeted column
	  * @param value Assigned value
	  * @param connection Implicit DB connection
	  * @return Whether any item was targeted
	  */
	def update(column: Column, value: Value)(implicit connection: Connection): Boolean
	
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
	// TODO: Also add withOrdering(Option[OrderBy]) and mapping functions
	def withOrdering(ordering: OrderBy): Repr
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param column Targeted column
	  * @return Data from the targeted column within the targeted data
	  */
	def apply(column: Column)(implicit connection: Connection): Val = apply(column, distinct = false)
	/**
	  * @param firstColumn First targeted column
	  * @param secondColumn Second targeted column
	  * @param moreColumns More targeted columns
	  * @param connection Implicit DB connection
	  * @return Targeted columns of the targeted item(s)
	  */
	def apply(firstColumn: Column, secondColumn: Column, moreColumns: Column*)(implicit connection: Connection): Seq[Val] =
		apply(Pair(firstColumn, secondColumn) ++ moreColumns)
	
	/**
	  * Clears the column (by setting it to NULL) value of all targeted items
	  * @param column Targeted column
	  * @param connection Implicit DB connection
	  * @return Whether any item was targeted
	  */
	def clear(column: Column)(implicit connection: Connection) = update(column, Value.empty)
	
	/**
	  * @param join First join to apply
	  * @param more More joins to apply
	  * @return A copy of this access which applies the specified joins
	  */
	def join(join: Joinable, more: Joinable*): Repr = this.join(Single(join) ++ more)
}
