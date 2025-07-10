package utopia.vault.sql

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.vault.model.immutable.{Column, Table, TableColumn}
import utopia.vault.model.template.Joinable
import utopia.vault.sql.JoinType._

import scala.util.Success

/**
  * This object is used for creating join sql segments that allow selection and manipulation of
  * multiple tables at once.
  * @author Mikko Hilpinen
  * @since 30.5.2017
  * @param from The column join is made from (in one of existing tables)
  * @param to The table and column that are joined
  * @param joinType The type of join used (default = Inner)
  * @param condition Condition that applies to joining (optional).
  *                  When specified, only rows satisfying the specified condition will participate in a join.
  */
case class Join(from: Column, to: TableColumn, joinType: JoinType = Inner, condition: Option[Condition] = None)
	extends Joinable
{
	// COMPUTED PROPERTIES    ----------------
	
	/**
	  * An sql segment based on this join (Eg. "LEFT JOIN table2 ON table1.column1 = table2.column2")
	  */
	def toSqlSegment = {
		val base = SqlSegment(s"$joinType JOIN ${ to.table.sqlName } ON ${ from.sqlName } = ${ to.sqlName }",
			Empty, Some(to.table.databaseName), Set(to.table))
		condition match {
			case Some(c) => base + "AND" + c.segment
			case None => base
		}
	}
	
	@deprecated("Renamed to .from", "v1.22")
	def leftColumn = from
	@deprecated("Please use .to.column instead", "v1.22")
	def rightColumn = to.column
	@deprecated("Please use .to.table instead", "v1.22")
	def rightTable = to.table
	
	/**
	  * The point targeted / included by this join
	  */
	@deprecated("Please use .to instead", "v1.22")
	def targetPoint = to
	
	
	// IMPLEMENTED  ----------------------
	
	override def toJoinsFrom(originTables: Seq[Table], joinType: JoinType = joinType) = {
		// If the original tables already contain this one, skips this join
		if (originTables.contains(to.table))
			Success(Empty)
		else
			Success(Single(this))
	}
	
	/**
	 * @param condition A condition that determines whether a join will be performed or not
	 * @return A copy of this join that only joins cases that satisfy the specified condition
	 */
	override def where(condition: Condition): Join = {
		val newCondition = this.condition match {
			case Some(c) => c && condition
			case None => condition
		}
		copy(condition = Some(newCondition))
	}
	
	
	// OTHER    --------------------------
	
	/**
	  * @param joinType Join type to use
	  * @return A copy of this join with that join type applied
	  */
	def withType(joinType: JoinType) = if (this.joinType == joinType) this else copy(joinType = joinType)
}