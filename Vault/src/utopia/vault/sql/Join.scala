package utopia.vault.sql

import utopia.vault.sql.JoinType._

import scala.collection.immutable.HashSet
import utopia.vault.model.immutable.{Column, ReferencePoint, Table}

object Join
{
    /**
     * Creates a join based on a reference
     */
    def apply(leftColumn: Column, target: ReferencePoint) = new Join(leftColumn, target.table, target.column)
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
}