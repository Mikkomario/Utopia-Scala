package utopia.vault.sql

import utopia.vault.model.immutable.{Column, Table}

/**
 * This object is used for generating select sql segments. If you wish to select all columns from a 
 * table, it is better to use SelectAll.
 * @author Mikko Hilpinen
 * @since 25.5.2017
 */
object Select
{
    // OPERATORS    -----------------
    
    /**
     * Creates an sql segment that selects a number of columns from a table
     */
    def apply(target: SqlTarget, columns: Seq[Column]) = SqlSegment(s"SELECT ${ 
        if (columns.isEmpty) "NULL" else columns.view.map { _.columnNameWithTable }.mkString(", ") } FROM",
        isSelect = true) + target.toSqlSegment
    
    /**
     * Creates an sql segment that selects a single column from a table
     */
    def apply(target: SqlTarget, column: Column): SqlSegment = apply(target, Vector(column))
    
    /**
     * Creates an sql segment that selects a number of columns from a table
     */
    def apply(target: SqlTarget, first: Column, second: Column, more: Column*): SqlSegment = 
            apply(target, Vector(first, second) ++ more)
    
    /**
     * Creates an sql segment that selects one or multiple properties from a single table
     */
    def apply(table: Table, firstName: String, moreNames: String*): SqlSegment = 
            apply(table, table(firstName +: moreNames))
    
    /**
     * Creates an sql segment that selects all columns of a single table from a larger selection target
     * @param target Selection target
     * @param selectedTable Table whose columns should be included in the final result
     * @return A new select segment
     */
    def apply(target: SqlTarget, selectedTable: Table): SqlSegment = apply(target, selectedTable.columns)
    
    
    // OTHER METHODS    -------------
    
    /**
     * Creates an sql segment that selects the primary key of a table
     */
    def index(table: Table) = apply(table, table.primaryColumn.toSeq)
    
    /**
     * @param target Selection target
     * @param table Table whose indices are selected
     * @return an sql segment that selects the primary key of a single table
     */
    def index(target: SqlTarget, table: Table) = apply(target, table.primaryColumn.toSeq)
    
    /**
     * Creates an sql segment that selects nothing from a table
     */
    def nothing(target: SqlTarget) = apply(target, Vector())
    
    /**
     * @param target Selection target
     * @param tables The tables from which data is read
     * @return A select segment that targets 'target' and selects data from 'tables'
     */
    def tables(target: SqlTarget, tables: Seq[Table]) = apply(target, tables.flatMap { _.columns })
}