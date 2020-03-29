package utopia.vault.sql

import utopia.flow.datastructure.template.Model
import utopia.flow.datastructure.template.Property

import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.immutable
import utopia.vault.model.immutable.Table

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
     * multiple tables, but must contain all tables that are introduced in the 'set'
     * @param set New value assignments for each of the modified tables. Property names are used 
     * as model keys, they will be converted to column names automatically
     * @return an update segment (select nothing segment if there's nothing to update)
     */
    def apply(target: SqlTarget, set: Map[Table, Model[Property]]) = 
    {
        val valueSet = set.flatMap { case (table, model) => model.attributes.flatMap { 
                property => table.find(property.name).map { (_, property.value) } } }
        
        if (valueSet.isEmpty)
            target.toSqlSegment.prepend("SELECT NULL FROM")
        else 
            target.toSqlSegment.prepend("UPDATE") + SqlSegment("SET " +
                valueSet.view.map { case (column, _) => column.columnNameWithTable + " = ?" }.mkString(", "),
                valueSet.values.toVector)
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