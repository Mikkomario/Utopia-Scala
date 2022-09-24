package utopia.vault.model.immutable

import utopia.flow.collection.immutable.Pair
import utopia.vault.sql.Join

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
     * This reference as a valid sql target that includes two tables
     */
    def toSqlTarget = from.table + Join(from.column, to)
    
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
}