package utopia.vault.sql

import utopia.vault.model.immutable.{Column, Table}
import utopia.vault.model.template.{HasTables, Joinable}
import utopia.vault.sql.JoinType._

/**
  * Sql targets are suited targets for operations like select, update and delete. A target may
  * consist of one or more tables and can always be converted to an sql segment when necessary
  */
trait SqlTarget extends HasTables
{
	// ABSTRACT METHODS    --------------------
	
	/**
	  * @return Name of the targeted database
	  */
	def databaseName: String
	
	/**
	  * Converts this sql target into a SQL segment
	  */
	def toSqlSegment: SqlSegment
	
	/**
	 * @param table A table
	 * @return Whether this target already contains that table
	 */
	def contains(table: Table): Boolean
	
	
	// OTHER    ----------------------------
	
	/**
	  * Joins another table to this target using by appending an already complete join
	  */
	def +(join: Join): SqlTarget = {
		// Will ignore joins to tables already contained within this target
		if (contains(join.to.table))
			this
		else
			SqlTargetWrapper(toSqlSegment + join.toSqlSegment, databaseName, tables :+ join.to.table)
	}
	
	/**
	  * Adds 0-n joins to this target
	  * @param joins Joins to append to this target
	  * @return Copy of this target with the joins included
	  */
	def ++(joins: Seq[Join]): SqlTarget = {
		if (joins.isEmpty)
			this
		else {
			// TODO: This filtering might be unnecessary now that Joinable.toJoinFrom is more carefully implemented
			val newJoins = joins.filterNot { join => contains(join.to.table) }
			if (newJoins.isEmpty)
				this
			else
				SqlTargetWrapper(toSqlSegment ++ newJoins.map { _.toSqlSegment }, databaseName,
					tables ++ newJoins.map { _.to.table })
		}
	}
	
	/**
	  * @param target A target which may be joined unto this sql target
	  * @param joinType Type of joining to use (default = inner)
	  * @return An extended copy of this target with the specified element joined
	  * @throws Exception If joining is impossible
	  */
	@throws[Exception]("If joining is impossible")
	def join(target: Joinable, joinType: JoinType = Inner) = this ++ target.toJoinsFrom(tables, joinType).get
	/**
	  * Includes a number of new elements to this target
	  * @param targets Targets to include
	  * @param joinType Applied join type (default = inner)
	  * @throws Exception If joining is impossible
	  * @return An extended copy of this target. This target, if joining was not necessary.
	  */
	@throws[Exception]("If joining is impossible")
	def join(targets: Iterable[Joinable], joinType: JoinType): SqlTarget = {
		if (targets.knownSize == 0)
			this
		else
			targets.foldLeft(this) { _.join(_, joinType) }
	}
	/**
	  * Includes a number of new elements to this target
	  * @param targets Targets to include
	  * @throws Exception If joining is impossible
	  * @return An extended copy of this target. This target, if joining was not necessary.
	  */
	@throws[Exception]("If joining is impossible")
	def join(targets: Iterable[Joinable]): SqlTarget = join(targets, Inner)
	
	/**
	  * Joins another table into this sql target based on a reference in the provided column.
	  * This will only work if the column belongs to one of the already targeted tables and
	  * the column references another column
	  */
	def joinFrom(column: Column, joinType: JoinType = Inner) = join(column, joinType)
}

private class CannotJoinTableException(message: String) extends RuntimeException(message)