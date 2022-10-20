package utopia.vault.model.immutable

import utopia.flow.collection.immutable.Pair
import utopia.vault.model.error.NoReferenceFoundException
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Join, JoinType}

object Reference
{
    /**
      * @param from Point where this reference is made from
      * @param to Referenced point
      * @return A reference between these two points
      */
    def apply(from: ReferencePoint, to: ReferencePoint): Reference = apply(Pair(from, to))
    
    /**
     * Creates a new reference
     */
    def apply(fromTable: Table, fromColumn: Column, toTable: Table, toColumn: Column): Reference =
            apply(ReferencePoint(fromTable, fromColumn), ReferencePoint(toTable, toColumn))
    
    /**
     * Creates a new reference by finding the proper columns from the tables
     * @return a reference between the tables. None if no proper column data was found
     */
    def apply(fromTable: Table, fromPropertyName: String, toTable: Table, toPropertyName: String): Option[Reference] =
    {
        val from = ReferencePoint(fromTable, fromPropertyName)
        val to = ReferencePoint(toTable, toPropertyName)
        
        if (from.isDefined && to.isDefined)
            Some(apply(from.get, to.get))
        else
            None
    }
}

/**
* A reference is a link from one column to another
* @author Mikko Hilpinen
* @since 21.5.2018
**/
case class Reference(points: Pair[ReferencePoint])
{
    // COMPUTED    ----------------------
    
    /**
      * @return The referencing point
      */
    def from = points.first
    /**
      * @return The referenced point
      */
    def to = points.second
    
    /**
      * @return A reverse of this reference.
      *         I.e. Starting from the column being pointed to and ending with the origin column.
      */
    def reverse = Reference(points.reverse)
    
    /**
     * This reference as a valid sql target that includes two tables
     */
    def toSqlTarget = from.table + Join(from.column, to)
    
    /**
      * @return An inner join based on this reference,
      *         in the same direction as this reference is
      */
    def toInnerJoin = toJoinOfType(Inner)
    /**
      * @return A left join based on this reference,
      *         in the same direction as this reference is
      */
    def toLeftJoin = toJoinOfType(JoinType.Left)
    /**
      * @return A right join based on this reference,
      *         in the same direction as this reference is
      */
    def toRightJoin = toJoinOfType(JoinType.Right)
    /**
      * Alias for .toInnerJoin
      * @return An inner join based on this reference,
      *         in the same direction as this reference is
      */
    def toJoin = toInnerJoin
    
    /**
     * The tables that are included in this reference
     */
    def tables = points.map { _.table }
    /**
     * The columns that are used by this reference
     */
    def columns = points.map { _.column }
    
    
    // IMPLEMENTED  ---------------------
    
    override def toString = s"$from -> $to"
    
    
    // OTHER    -------------------------
    
    /**
      * Converts this reference to a join
      * @param joinType Join type to use
      * @return A join based on this reference, in the same direction as this reference
      */
    def toJoinOfType(joinType: JoinType) = Join(from.column, to, joinType)
    /**
      * Converts this reference to a join, starting from the specified table
      * @param table A table from which to start the join
      * @param joinType Type of join to apply (default = Inner)
      * @throws NoReferenceFoundException If the specified table is not part of this reference
      * @return A join from 'table' to the other end-point in this reference
      */
    @throws[NoReferenceFoundException]("If the specified table is not part of this reference")
    def toJoinFrom(table: Table, joinType: JoinType = Inner) = {
        if (from.table == table)
            toJoinOfType(joinType)
        else if (to.table == table)
            Join(to.column, from, joinType)
        else
            throw new NoReferenceFoundException(s"Reference $this can't join from $table")
    }
}