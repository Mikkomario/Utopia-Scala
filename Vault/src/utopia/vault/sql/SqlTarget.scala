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
     * Converts this sql target into an sql segment
     */
    def toSqlSegment: SqlSegment
    
    /**
      * @return Tables contained within this target
      */
    def tables: Vector[Table]
    
    
    // OPERATORS    ----------------------------
    
    /**
     * Joins another table to this target using by appending an already complete join
     */
    def +(join: Join): SqlTarget = SqlTargetWrapper(toSqlSegment + join.toSqlSegment, tables :+ join.rightTable)
    
    
    // OTHER METHODS    ------------------------
    
    /**
      * @param target A target which may be joined unto this sql target
      * @param joinType Type of joining to use (default = inner)
      * @return An extended copy of this target with the specified element joined
      * @throws Exception If joining is impossible
      */
    @throws[Exception]("If joining is impossible")
    def join(target: Joinable, joinType: JoinType = Inner) = this + target.toJoinFrom(tables, joinType).get
    
    /**
     * Joins another table into this sql target based on a reference in the provided column. 
     * This will only work if the column belongs to one of the already targeted tables and 
     * the column references another column
     */
    def joinFrom(column: Column, joinType: JoinType = Inner) = join(column, joinType)
}

private class CannotJoinTableException(message: String) extends RuntimeException(message)