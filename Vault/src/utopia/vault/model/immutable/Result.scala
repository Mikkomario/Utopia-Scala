package utopia.vault.model.immutable

import utopia.flow.generic.model.immutable.Value
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.MaybeEmpty
import utopia.vault.nosql.factory.row.FromRowFactory

object Result
{
    /**
      * An empty result
      */
    val empty = Result()
}

/**
 * A result is generated based on the data retrieved from a executed database query. 
 * Usually results are used for accessing read database row data, but in case of an insert
 * statement, the generated indices are also available.
 * @author Mikko Hilpinen
 * @since 25.4.2017
  * @param rows The retrieved data rows (on select)
  * @param generatedKeys Primary keys of newly generated rows (on insert)
  * @param updatedRowCount Number of updated rows (on update)
 */
case class Result(rows: Vector[Row] = Vector(), generatedKeys: Vector[Value] = Vector(), updatedRowCount: Int = 0)
    extends MaybeEmpty[Result]
{
    // COMPUTED PROPERTIES    ------------
    
    /**
     * The row data in model format. This should be used when no joins were present in the query and
     * each row contains data from a single table only
     */
    def rowModels = rows.map { _.toModel }
    
    /**
     * @return All rows converted into values. Should only be used for results where each row only contains a single
     *         value
     */
    def rowValues = rows.map { _.value }
    
    /**
     * @return All rows converted into integer values (empty rows excluded). Should only be used for results where each
     *         row consists of a single integer value.
     */
    def rowIntValues = rowValues.flatMap { _.int }
    
    /**
     * The data of the first result row in model format. This should be used only when no joins 
     * were present in the query and each row contains data from a single table only
     */
    def firstModel = rows.headOption.map { _.toModel }
    
    /**
      * @return The first value in this result. Should only be used when a single column is selected and query is
      *         limited to 1 row
      */
    def firstValue = rows.headOption.map { _.value } getOrElse Value.empty
    
    /**
     * The generated keys in integer format
     */
    def generatedIntKeys = generatedKeys.flatMap { _.int }
    
    /**
     * The generated keys in long format
     */
    def generatedLongKeys = generatedKeys.flatMap { _.long }
    
    /**
      * @return All indices within this result. Won't work properly when rows contain indices from multiple tables.
      */
    def indices = rows.map { _.index }
    
    /**
      * @return The index of the first result row
      */
    def firstIndex = rows.headOption.map { _.index } getOrElse Value.empty
    
    /**
      * @return Whether the query updated any rows
      */
    def updatedRows = updatedRowCount > 0
    
    
    // IMPLEMENTED  ----------------------
    
    override def self = this
    
    /**
      * Whether this result is empty and doesn't contain any rows or generated keys
      */
    override def isEmpty = generatedKeys.isEmpty && rows.isEmpty
    
    override def toString =
    {
        if (generatedKeys.nonEmpty)
            s"Generated keys: [${ generatedKeys.map { _.getString }.mkString(", ") }]"
        else if (updatedRowCount > 0)
            s"Updated $updatedRowCount row(s)"
        else if (rows.nonEmpty)
            s"${rows.size} Row(s): \n${rows.map { "\t" + _ }.mkString("\n")}"
        else
            "Empty result"
    }
    
    
    // OTHER METHODS    ------------------
    
    /**
     * Parses data from each available row
     * @param factory Factory used for parsing data
     * @tparam A Type of parse result per row
     * @return All successfully parsed models
     */
    def parse[A](factory: FromRowFactory[A]) = rows.flatMap(factory.parseIfPresent)
    
    /**
     * Parses data from up to one row
     * @param factory Factory used for parsing data
     * @tparam A Type of parse result
     * @return Parsed result or None if parsing failed or no data was available
     */
    def parseSingle[A](factory: FromRowFactory[A]) = rows.findMap(factory.parseIfPresent)
    
    /**
     * Retrieves row data concerning a certain table
     * @param table The table whose data is returned
     */
    def rowsForTable(table: Table) = rows.flatMap { _.columnData.get(table) }
    
    /**
      * @param table Target table
      * @return Index results for specified table
      */
    def indicesForTable(table: Table) = rows.map { _.indexForTable(table) }
    
    /**
      * @param table Target table
      * @return The first row index for the specified table
      */
    def firstIndexForTable(table: Table) = rows.headOption.map { _.indexForTable(table) } getOrElse Value.empty
    
    /**
      * Groups the rows to groups by tables
      * @param primaryTable The table the grouping is primarily done
      * @param secondaryTables The tables that have additional row groups
      * @return A map that links a group of rows to each unique primary table index. The primary table's row is also linked.
      *         The secondary maps contain rows for each of the secondary tables (although the list may be empty).
      *         Only unique rows are preserved (based on row index)
      */
    def grouped(primaryTable: Table, secondaryTables: Iterable[Table]) =
    {
        rows.filter { _.containsDataForTable(primaryTable) }.groupBy { _.indexForTable(primaryTable) }.view
            .mapValues { rows =>
                rows.head -> secondaryTables.map { table =>
                    table -> rows.filter { _.containsDataForTable(table) }.distinctBy { _.indexForTable(table) }
                }.toMap
            }.toMap
    }
    
    /**
      * Groups the rows by a table
      * @param primaryTable The table that determines the groups
      * @param secondaryTable The table that is dependent / linked to the first table
      * @return A map that contains links to a list of rows for each unique primary table index. Primary table row
      *         is also included in results. Resulting lists contain only rows that include data from the secondary table,
      *         each duplicate row (based on secondary table index) is removed.
      */
    def grouped(primaryTable: Table, secondaryTable: Table) =
    {
        rows.filter { _.containsDataForTable(primaryTable) }.groupBy { _.indexForTable(primaryTable) }
            .view.mapValues { rows => rows.head -> rows.filter { _.containsDataForTable(secondaryTable) }
                .distinctBy { _.indexForTable(secondaryTable) } }.toMap
    }
    
    /**
     * Divides this result into multiple sub-results based on a table id
     * @param primaryTable The table based on which the this result is split
     * @return Sub-results found. Please note that this won't include any results / rows without data from the primary table.
     */
    def split(primaryTable: Table) =
    {
        val rowsPerId = rows.filter { _.containsDataForTable(primaryTable) }.groupBy { _.indexForTable(primaryTable) }
        rowsPerId.values.map { rows => copy(rows = rows) }.toVector
    }
}