package utopia.vault.sql

import utopia.vault.sql.JoinType._

import scala.collection.immutable.HashSet
import utopia.vault.model.immutable.{Column, ReferencePoint, Table}
import utopia.vault.model.template.Joinable

import scala.util.Success

object Join
{
    /**
     * Creates a join based on a reference
     */
    def apply(leftColumn: Column, target: ReferencePoint) = new Join(leftColumn, target.table, target.column)
    
    /**
      * Creates a join based on a reference
      * @param leftColumn Left side joined column
      * @param target Right side target for that join
      * @param joinType Type of join used (default = inner)
      * @return A new join
      */
    def apply(leftColumn: Column, target: ReferencePoint, joinType: JoinType): Join =
        new Join(leftColumn, target.table, target.column, joinType)
}

/**
 * This object is used for creating join sql segments that allow selection and manipulation of 
 * multiple tables at once.
 * @author Mikko Hilpinen
 * @since 30.5.2017
  * @param leftColumn The column join is made from (in one of existing tables)
  * @param rightTable The table that is joined
  * @param rightColumn A column in rightTable that should match the leftColumn
  * @param joinType The type of join used (default = Inner)
 */
case class Join(leftColumn: Column, rightTable: Table, rightColumn: Column, joinType: JoinType = Inner)
    extends Joinable
{
    // COMPUTED PROPERTIES    ----------------
    
    /**
     * An sql segment based on this join (Eg. "LEFT JOIN table2 ON table1.column1 = table2.column2")
     */
    def toSqlSegment =
    {
        SqlSegment(s"$joinType JOIN ${rightTable.sqlName} ON ${
            leftColumn.columnNameWithTable } = ${rightColumn.columnNameWithTable}", Vector(),
            Some(rightTable.databaseName), HashSet(rightTable))
    }
    
    /**
     * The point targeted / included by this join
     */
    def targetPoint = ReferencePoint(rightTable, rightColumn)
    
    
    // IMPLEMENTED  ----------------------
    
    override def toJoinFrom(originTables: Vector[Table], joinType: JoinType = joinType) = Success(this)
    
    
    // OTHER    --------------------------
    
    /**
      * @param joinType Join type to use
      * @return A copy of this join with that join type applied
      */
    def withType(joinType: JoinType) = if (this.joinType == joinType) this else copy(joinType = joinType)
}