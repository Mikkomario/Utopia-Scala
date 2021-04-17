package utopia.vault.sql

import utopia.flow.datastructure.template.Model
import utopia.flow.datastructure.template.Property
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Result, Table}

import scala.collection.immutable.HashSet

/**
 * Insert object is used for generating insert statements that can then be executed with a 
 * suitable database connection
 * @author Mikko Hilpinen
 * @since 22.5.2017
 */
object Insert
{
    /**
     * Inserts multiple rows into an sql database. This statement is not combined with other statements and targets a
      * single table only
     * @param table the table into which the rows are inserted
     * @param rows models representing rows in the table. Only properties with values and which 
     * match those of the table are used
     * @return Results of the insert operation, which contain generated auto-increment keys where applicable
     */
    def apply(table: Table, rows: Seq[Model[Property]])(implicit connection: Connection) =
    {
        if (rows.isEmpty)
            Result.empty
        else
        {
            // Finds the inserted properties that are applicable to this table
            // Only properties matching columns (that are not auto-increment) are included
            val insertedPropertyNames = rows.flatMap { _.attributesWithValue.map { _.name } }.toSet.filter {
                table.find(_).exists { !_.usesAutoIncrement } }.toVector
    
            val columnNames = insertedPropertyNames.map { table(_).sqlColumnName }.mkString(", ")
            val singleRowValuesSql =
            {
                if (insertedPropertyNames.nonEmpty)
                    "(?" + ", ?" * (insertedPropertyNames.size - 1) + ")"
                else
                    "()"
            }
            val valuesSql = singleRowValuesSql + (", " + singleRowValuesSql) * (rows.size - 1)
            val values = rows.flatMap { model => insertedPropertyNames.map { model(_) } }
    
            val segment = SqlSegment(s"INSERT INTO ${table.sqlName} ($columnNames) VALUES $valuesSql", values,
                Some(table.databaseName), HashSet(table), generatesKeys = table.usesAutoIncrement)
    
            connection(segment)
        }
    }
    
    /**
     * A convenience method for executing an insert statement for a single row.
     * @param table The table to which the row is inserted
     * @param row the row that is inserted to the table. Only properties matching table columns are
     * used
     * @return Results of the insert, which may contain a possibly generated auto-increment key (if applicable)
     */
    def apply(table: Table, row: Model[Property])(implicit connection: Connection): Result = apply(table, Vector(row))
    
    /**
     * Inserts multiple rows into an sql database. This statement is not combined with other statements and targets a
      * single table only
     * @param table the table into which the rows are inserted
     * @return Results of the insert operation, which contain generated auto-increment keys where applicable
     */
    def apply(table: Table, first: Model[Property], second: Model[Property], more: Model[Property]*)
             (implicit connection: Connection): Result = apply(table, Vector(first, second) ++ more)
}