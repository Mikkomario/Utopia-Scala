package utopia.vault.sql

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Table
import utopia.vault.model.immutable.TableUpdateEvent.DataDeleted

/**
 * This object is used for creating sql statements which delete contents from the database. 
 * Delete statements can be used alongside join- and where clauses, too.
 * @author Mikko Hilpinen
 * @since 22.5.2017
 */
object Delete
{
    /**
     * Creates an sql segment that deletes rows from possibly multiple tables. All of the deleted 
     * tables must be included in the provided target.
     * @param target The target for the delete operation, may be a single table or join, but must 
     * contain all deleted tables.
     * @param deletedTables The tables from which rows are deleted (shouldn't be empty)
     */
    def apply(target: SqlTarget, deletedTables: Seq[Table]) = {
        if (deletedTables.isEmpty)
            SqlSegment("SELECT NULL FROM") + target.toSqlSegment
        else
            SqlSegment(s"DELETE ${ deletedTables.map { _.sqlName }.mkString(", ") } FROM",
                events = Some(_ => deletedTables.map { table => DataDeleted(table) })) +
                target.toSqlSegment
    }
    
    /**
      * Creates an sql segment that deletes rows from a single table. The deleted
      * table must be included in the provided target.
      * @param target The target for the delete operation, may be a single table or join, but must
      * contain the deleted table.
      * @param table The table from which rows are deleted
      */
    def apply(target: SqlTarget, table: Table): SqlSegment = apply(target, Single(table))
    /**
     * Creates an sql segment that deletes rows from a single table. This segment is often followed 
     * by a where clause and possibly a limit as well.
     */
    def apply(target: SqlTarget): SqlSegment = apply(target, target.toSqlSegment.targetTables.toSeq)
    /**
     * @param table Table being targeted by this delete statement
     * @return A delete statement targeting that table
     */
    def apply(table: Table) = SqlSegment("DELETE FROM", events =
        Some(_ => Single(DataDeleted(table)))) + table.toSqlSegment
    
    /**
     * Performs one or more delete queries on a table in a way that deletes only a certain number of items per query.
     * This is in order to avoid extremely large deletions.
     * @param table Table being targeted
     * @param deletionsPerQuery Maximum number of deletions on a single query
     * @param where A condition to apply to the queries
     * @param connection Database connection (implicit)
     * @return Total number of deleted rows
     */
    def inParts(table: Table, deletionsPerQuery: Int, where: Option[Condition] = None)
               (implicit connection: Connection) =
    {
        val statement = apply(table) + where.map { Where(_) } + Limit(deletionsPerQuery)
        Iterator.continually { connection(statement) }
            .takeTo { _.updatedRowCount < deletionsPerQuery }
            .map { _.updatedRowCount }.sum
    }
}