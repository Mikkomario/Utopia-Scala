package utopia.vault.sql

import utopia.flow.collection.CollectionExtensions._
import utopia.vault.model.immutable.{Column, Table}

/**
 * This object is used for generating select sql segments. If you wish to select all columns from a 
 * table, it is better to use SelectAll.
 * @author Mikko Hilpinen
 * @since 25.5.2017
 */
object Select
{
    // OTHER    -----------------
    
    /**
     * Creates an sql segment that selects a number of columns from a table
     */
    def apply(target: SqlTarget, columns: Iterable[Column]) = {
        val tables = target.tables
        if (columns.hasSize(tables.view.map { _.columns.size }.sum))
            all(target)
        else
            SqlSegment(s"SELECT ${
                if (columns.isEmpty) "NULL" else columns.view.map { _.columnNameWithTable }.mkString(", ") } FROM",
                isSelect = true) + target.toSqlSegment
    }
    
    /**
     * Creates an sql segment that selects a single column from a table
     */
    def apply(target: SqlTarget, column: Column): SqlSegment = _apply(target, Iterable.single(column))
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
    def apply(target: SqlTarget, selectedTable: Table): SqlSegment = _apply(target, selectedTable.columns)
    
    /**
      * Creates an sql segment that is used for retrieving data from all the columns from the
      * targeted rows. This statement can then be followed by a join- or where clause and possibly
      * a limit
      */
    def all(target: SqlTarget) = SqlSegment(s"SELECT * FROM", isSelect = true) + target.toSqlSegment
    
    /**
     * Creates an sql segment that selects the primary key of a table
     */
    def index(table: Table) = _apply(table, table.primaryColumn)
    /**
     * @param target Selection target
     * @param table Table whose indices are selected
     * @return an sql segment that selects the primary key of a single table
     */
    def index(target: SqlTarget, table: Table) = _apply(target, table.primaryColumn)
    
    /**
     * Creates an sql segment that selects nothing from a table
     */
    def nothing(target: SqlTarget) = _apply(target, Vector.empty)
    
    /**
     * @param target Selection target
     * @param tables The tables from which data is read
     * @return A select segment that targets 'target' and selects data from 'tables'
     */
    def tables(target: SqlTarget, tables: Seq[Table]) = {
        if (target.tables.forall(tables.contains))
            all(target)
        else
            _apply(target, tables.flatMap { _.columns })
    }
    
    // This version will not attempt to convert into SELECT *
    private def _apply(target: SqlTarget, columns: Iterable[Column]) = {
        SqlSegment(s"SELECT ${
            if (columns.isEmpty) "NULL" else columns.view.map { _.columnNameWithTable }.mkString(", ") } FROM",
            isSelect = true) + target.toSqlSegment
    }
}