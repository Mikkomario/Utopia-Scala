package utopia.vault.sql

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
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
     * @param target Targeted group of tables, etc.
      * @param columns Selected columns
      * @return A select statement for retrieving the specified columns
     */
    def apply(target: SqlTarget, columns: Iterable[Column]) = _apply(target, columnsTargetFrom(target, columns))
    
    /**
     * Creates an sql segment that selects a single column from a table
     */
    def apply(target: SqlTarget, column: Column): SqlSegment = _apply(target, singleColumnTargetFrom(target, column))
    /**
     * Creates an sql segment that selects a number of columns from a table
     */
    def apply(target: SqlTarget, first: Column, second: Column, more: Column*): SqlSegment = 
            apply(target, Pair(first, second) ++ more)
    
    /**
      * @param table Targeted table
      * @param propName Name of the targeted DB property in the targeted table
      * @return A select statement for retrieving values of the targeted property
      */
    def apply(table: Table, propName: String): SqlSegment = apply(table, table(propName))
    /**
     * Creates an sql segment that selects one or multiple properties from a single table
     */
    def apply(table: Table, firstName: String, secondName: String, moreNames: String*): SqlSegment =
            apply(table, table(Pair(firstName, secondName) ++ moreNames))
            
    /**
     * Creates an sql segment that selects all columns of a single table from a larger selection target
     * @param target Selection target
     * @param selectedTable Table whose columns should be included in the final result
     * @return A new select segment
     */
    def apply(target: SqlTarget, selectedTable: Table): SqlSegment = apply(target, selectedTable.columns)
    
    /**
      * Creates an sql segment that is used for retrieving data from all the columns from the
      * targeted rows. This statement can then be followed by a join- or where clause and possibly
      * a limit
      */
    def all(target: SqlTarget) = _apply(target, "*")
    
    /**
      * @param target Target of this query
      * @param column Targeted column
      * @return an SQL segment for selecting distinct values of 'column'
      */
    def distinct(target: SqlTarget, column: Column) =
        _apply(target, s"DISTINCT ${ singleColumnTargetFrom(target, column) }")
    /**
      * @param table Targeted table
      * @param propName Name of the targeted database property
      * @return an SQL segment for selecting distinct values of the targeted property
      */
    def distinct(table: Table, propName: String): SqlSegment = distinct(table, table(propName))
    
    /**
     * Creates an sql segment that selects the primary key of a table
     */
    def index(table: Table) = apply(table, table.primaryColumn)
    /**
     * @param target Selection target
     * @param table Table whose indices are selected
     * @return an sql segment that selects the primary key of a single table
     */
    def index(target: SqlTarget, table: Table) = apply(target, table.primaryColumn)
    
    /**
     * Creates an sql segment that selects nothing from a table
     */
    def nothing(target: SqlTarget) = _apply(target, "NULL")
    
    /**
     * @param target Selection target
     * @param tables The tables from which data is read
     * @return A select segment that targets 'target' and selects data from 'tables'
     */
    def tables(target: SqlTarget, tables: Seq[Table]) = {
        if (target.tables.forall(tables.contains))
            all(target)
        else
            apply(target, tables.flatMap { _.columns })
    }
    
    // Converts to the optimal select statement
    @deprecated
    private def _apply(target: SqlTarget, columns: Iterable[Column]) = {
        val columnsPart = {
            // Case: Not targeting any columns => Selects NULL
            if (columns.isEmpty)
                "NULL"
            else {
                val tables = target.tables
                // Case: Targeting all columns in the target => Uses * instead of listing all the columns
                if (columns.hasSize(tables.view.map { _.columns.size }.sum))
                    "*"
                // Case: Only targeting a single table => Doesn't use table prefixes
                else if (tables.hasSize(1))
                    columns.view.map { c => s"`${ c.columnName }`" }.mkString(", ")
                // Case: Targets multiple tables => Uses table prefixes
                else
                    columns.view.map { _.columnNameWithTable }.mkString(", ")
            }
        }
        SqlSegment(s"SELECT $columnsPart FROM", isSelect = true) + target.toSqlSegment
    }
    
    private def _apply(target: SqlTarget, columnsPart: String) =
        SqlSegment(s"SELECT $columnsPart FROM", isSelect = true) + target.toSqlSegment
    
    // Converts to the optimal select statement
    // Only includes the part that lists the selected columns
    private def columnsTargetFrom(target: SqlTarget, columns: Iterable[Column]) = {
        // Case: Not targeting any columns => Selects NULL
        if (columns.isEmpty)
            "NULL"
        else {
            val tables = target.tables
            // Case: Targeting all columns in the target => Uses * instead of listing all the columns
            if (columns.hasSize(tables.view.map { _.columns.size }.sum))
                "*"
            // Case: Only targeting a single table => Doesn't use table prefixes
            else if (tables.hasSize(1))
                columns.view.map { c => s"`${ c.columnName }`" }.mkString(", ")
            // Case: Targets multiple tables => Uses table prefixes
            else
                columns.view.map { _.columnNameWithTable }.mkString(", ")
        }
    }
    
    private def singleColumnTargetFrom(target: SqlTarget, column: Column) = {
        if (target.tables.hasSize(1))
            s"`${ column.columnName }`"
        else
            column.columnNameWithTable
    }
}