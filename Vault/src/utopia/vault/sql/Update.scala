package utopia.vault.sql

import utopia.flow.datastructure.immutable
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.{Model, Property}
import utopia.vault.database.columnlength.{ColumnLengthLimits, ColumnLengthRules}
import utopia.vault.model.immutable.TableUpdateEvent.RowsUpdated
import utopia.vault.model.immutable.{Column, Table}

import scala.collection.immutable.HashMap

/**
 * This object is used for generating update statements that modify database row contents
 * @author Mikko Hilpinen
 * @since 25.5.2017
 */
object Update
{
    // OPERATORS    ----------------------
    
    /**
     * Creates an sql segment that updates one or multiple tables
     * @param target The target portion for the update segment. This may consist of a single or
     * multiple tables, but must contain all tables that are affected by the 'set'
     * @param set Column value pairs that will be updated
     * @return an update segment (select nothing segment if there's nothing to update)
     */
    def columns(target: SqlTarget, set: Map[Column, Value]) =
    {
        if (set.isEmpty)
            target.toSqlSegment.prepend("SELECT NULL FROM")
        else
        {
            // Makes sure the specified values conform to the applicable column length limits
            val dbName = target.databaseName
            val orderedSet = set.toVector.map { case (column, rawValue) =>
                val modifiedValue = ColumnLengthLimits(dbName, column) match {
                    case Some(limit) => ColumnLengthRules(dbName, column)(dbName, column, limit, rawValue)
                    case None => rawValue
                }
                column -> modifiedValue
            }
            target.toSqlSegment.prepend("UPDATE") +
                SqlSegment("SET " +
                    orderedSet.view.map { case (column, _) => column.columnNameWithTable + " = ?" }.mkString(", "),
                    orderedSet.map { _._2 },
                    // Generates update events for all affected tables
                    events = Some(result => set.keySet.map { _.tableName }.toVector
                        .map { tableName => RowsUpdated(tableName, result.updatedRowCount) }))
        }
    }
    
    /**
     * Creates an sql segment that updates one or multiple tables
     * @param target The target portion for the update segment. This may consist of a single or 
     * multiple tables, but must contain all tables that are introduced in the 'set'
     * @param set New value assignments for each of the modified tables. Property names are used 
     * as model keys, they will be converted to column names automatically
     * @return an update segment (select nothing segment if there's nothing to update)
     */
    def apply(target: SqlTarget, set: Map[Table, Model[Property]]) = 
    {
        val valueSet = set.flatMap { case (table, model) => model.attributes.flatMap { 
                property => table.find(property.name).map { (_, property.value) } } }
        columns(target, valueSet)
    }
    
    /**
     * Creates an update segment that changes multiple values in a table
     * @return an update segment (select nothing segment if there's nothing to update)
     */
    def apply(table: Table, set: Model[Property]): SqlSegment = apply(table, HashMap(table -> set))
    
    /**
     * Creates an update segment that changes the value of a single column in the table
     * @return an update segment (select nothing segment if there's nothing to update)
     */
    def apply(table: Table, key: String, value: Value): SqlSegment = apply(table, immutable.Model(Vector(key -> value)))
    
    /**
     * Creates an update segment that updates the value of an individual column
     * @param target Targeted table / tables
     * @param column Column to update (should be part of the target)
     * @param value Value to assign for the column
     * @return A new update segment
     */
    def apply(target: SqlTarget, column: Column, value: Value) = columns(target, Map(column -> value))
    
    /**
     * Creates an update segment that changes multiple values in a table
     * @param target Update target (includes table & other tables used in conditions etc.)
     * @param table Table that is being updated
     * @param set Set of changes for the table
     * @return An update segment (select nothing segment if there's nothing to update)
     */
    def apply(target: SqlTarget, table: Table, set: Model[Property]): SqlSegment = apply(target, HashMap(table -> set))
    
    /**
     * Creates an update segment that changes a single value in a table
     * @param target Update target (includes table & other tables used in conditions etc.)
     * @param table Table that is being updated
     * @param key Name of updated attribute
     * @param value New value for the attribute
     * @return An update segment (select nothing segment if there's nothing to update)
     */
    def apply(target: SqlTarget, table: Table, key: String, value: Value): SqlSegment = apply(target, table,
        immutable.Model(Vector(key -> value)))
}