package utopia.vault.sql

import utopia.vault.model.immutable.{Column, Table}
import utopia.vault.model.template.Joinable
import utopia.vault.sql.JoinType._

/**
 * Sql targets are suited targets for operations like select, update and delete. A target may 
 * consist of one or more tables and can always be converted to an sql segment when necessary
 */
trait SqlTarget
{
    // ABSTRACT METHODS    --------------------
    
    /**
      * @return Name of the targeted database
      */
    def databaseName: String
    /**
      * @return Tables contained within this target
      */
    def tables: Vector[Table]
    
    /**
      * Converts this sql target into an sql segment
      */
    def toSqlSegment: SqlSegment
    
    
    // OPERATORS    ----------------------------
    
    /**
     * Joins another table to this target using by appending an already complete join
     */
    def +(join: Join): SqlTarget = {
        val existingTables = tables
        // Will ignore joins to tables already contained within this target
        if (existingTables.contains(join.rightTable))
            this
        else
            SqlTargetWrapper(toSqlSegment + join.toSqlSegment, databaseName, existingTables :+ join.rightTable)
    }
    
    /**
      * Adds 0-n joins to this target
      * @param joins Joins to append to this target
      * @return Copy of this target with the joins included
      */
    def ++(joins: Seq[Join]): SqlTarget = {
        val existingTables = tables
        val newJoins = joins.filterNot { join => existingTables.contains(join.rightTable) }
        if (newJoins.isEmpty)
            this
        else
            SqlTargetWrapper(toSqlSegment ++ newJoins.map { _.toSqlSegment }, databaseName,
                existingTables ++ newJoins.map { _.rightTable })
    }
    
    
    // OTHER METHODS    ------------------------
    
    /**
      * @param target A target which may be joined unto this sql target
      * @param joinType Type of joining to use (default = inner)
      * @return An extended copy of this target with the specified element joined
      * @throws Exception If joining is impossible
      */
    @throws[Exception]("If joining is impossible")
    def join(target: Joinable, joinType: JoinType = Inner) = this ++ target.toJoinsFrom(tables, joinType).get
    
    /**
     * Joins another table into this sql target based on a reference in the provided column. 
     * This will only work if the column belongs to one of the already targeted tables and 
     * the column references another column
     */
    def joinFrom(column: Column, joinType: JoinType = Inner) = join(column, joinType)
}

private class CannotJoinTableException(message: String) extends RuntimeException(message)