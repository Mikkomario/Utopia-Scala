package utopia.vault.model.immutable

import utopia.vault.sql.Join

object Reference
{
    /**
     * Creates a new reference
     */
    def apply(fromTable: Table, fromColumn: Column, toTable: Table, toColumn: Column) = 
            new Reference(ReferencePoint(fromTable, fromColumn), ReferencePoint(toTable, toColumn))
    
    /**
     * Creates a new reference by finding the proper columns from the tables
     * @return a reference between the tables. None if no proper column data was found
     */
    def apply(fromTable: Table, fromPropertyName: String, toTable: Table, toPropertyName: String) = 
    {
        val from = ReferencePoint(fromTable, fromPropertyName)
        val to = ReferencePoint(toTable, toPropertyName)
        
        if (from.isDefined && to.isDefined)
            Some(new Reference(from.get, to.get))
        else
            None
    }
}

/**
* A reference is a link from one column to another
* @author Mikko Hilpinen
* @since 21.5.2018
**/
case class Reference(from: ReferencePoint, to: ReferencePoint)
{
    // COMPUTED    ----------------------
    
    /**
     * This reference as a valid sql target that includes two tables
     */
    def toSqlTarget = from.table + Join(from.column, to)
    
    /**
     * The tables that are included in this reference
     */
    def tables = Vector(from.table, to.table)
    
    /**
     * The columns that are used by this reference
     */
    def columns = Vector(from.column, to.column)
    
    
    // IMPLEMENTED  ---------------------
    
    override def toString = s"$from -> $to"
}