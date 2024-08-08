package utopia.vault.model.immutable

import utopia.flow.generic.model.immutable.Value
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
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
// TODO: In generatedKeys, it might be good to allow the use of IntSet
case class Result(rows: Seq[Row] = Empty, generatedKeys: Seq[Value] = Empty, updatedRowCount: Int = 0)
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
    @deprecated("Please use groupAnd(...) instead", "v1.18")
    def grouped(primaryTable: Table, secondaryTables: Iterable[Table]) = {
        rows.filter { _.containsDataForTable(primaryTable) }.groupBy { _.indexForTable(primaryTable) }.view
            .mapValues { rows =>
                rows.head ->
                    secondaryTables.map { table =>
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
    @deprecated("Please use group(...) instead", "v1.18")
    def grouped(primaryTable: Table, secondaryTable: Table) = {
        rows.filter { _.containsDataForTable(primaryTable) }.groupBy { _.indexForTable(primaryTable) }
            .view.mapValues { rows =>
                rows.head -> rows.filter { _.containsDataForTable(secondaryTable) }
                    .distinctBy { _.indexForTable(secondaryTable) }
            }
            .toMap
    }
    
    /**
      * Combines parsed data from this result, using two factories and one merge function
      * @param f1 Factory for reading the primary elements
      * @param f2 Factory for reading the secondary elements
      * @param merge Function that joins/merges two parsed elements together
      * @tparam P Type of the primary parsed elements
      * @tparam C Type of the secondary parsed elements
      * @tparam R Type of the merge results
      * @return Merge results
      */
    def combine[P, C, R](f1: FromRowFactory[P], f2: FromRowFactory[C])(merge: (P, C) => R) = {
        val tables = Pair(f1, f2).map { _.table }
        rows.view
            // Ignores rows that don't contain valid data
            .filter { row => tables.forall(row.containsDataForTable) }
            // Parses the models separately for each row and then joins them
            .flatMap { row => f1.tryParse(row).flatMap { parent => f2.tryParse(row).map { merge(parent, _) } } }
            .toVector
    }
    /**
      * Combines parsed data from this result, using three factories and a merge function
      * @param f1 Factory for reading the primary elements
      * @param f2 Factory for reading the secondary elements
      * @param f3 Factory for reading the tertiary elements
      * @param merge Function that joins/merges three parsed elements together
      * @tparam P Type of the primary parsed elements
      * @tparam C1 Type of the secondary parsed element
      * @tparam C2 Type of the tertiary parsed element
      * @tparam R Type of the merge results
      * @return Merge results
      */
    def combine[P, C1, C2, R](f1: FromRowFactory[P], f2: FromRowFactory[C1], f3: FromRowFactory[C2])
                             (merge: (P, C1, C2) => R) =
    {
        val tables = Vector(f1.table, f2.table, f3.table)
        rows.view.filter { row => tables.forall(row.containsDataForTable) }.flatMap { row =>
            f1.tryParse(row).flatMap { parent =>
                f2.tryParse(row).flatMap { child1 => f3.tryParse(row).map { merge(parent, child1, _) } }
            }
        }.toVector
    }
    /**
      * Groups the results around unique primary entries.
      * Also parses and joins the unique secondary entries found for each primary entry.
      * @param parentFactory Factory used for parsing the primary entries
      * @param childFactory Factory used for parsing the secondary entries
      * @param merge Function that marges the grouped second-level parse results with the primary entry
      * @tparam P Type of primary parse results
      * @tparam C Type of secondary parse results
      * @tparam R Type of merge results
      * @return Merge results
      */
    def group[P, C, R](parentFactory: FromRowFactory[P], childFactory: FromRowFactory[C])(merge: (P, Vector[C]) => R) = {
        val tp = parentFactory.table
        val tc = childFactory.table
        // Processes all rows concerning a single parent index as a single group
        rows.filter { _.containsDataForTable(tp) }.groupBy { _.indexForTable(tp) }.flatMap { case (_, rows) =>
            parentFactory.tryParse(rows.head).map { parent =>
                val children = rows.view
                    .filter { _.containsDataForTable(tc) }
                    // Makes sure the secondary entries are unique
                    .distinctBy { _.indexForTable(tc) }
                    .flatMap(childFactory.tryParse)
                    .toVector
                merge(parent, children)
            }
        }
    }
    /**
      * Performs grouped parsing on three levels:
      * - First groups the rows based on primary merge results
      * - Then groups the sub-groups based on the secondary merge results
      * - Finally parses the tertiary entries using the specified factory
      *
      * Joins the results together in two levels:
      * - First joins the tertiary entries with the secondary entries
      * - Then joins these merge results with the associated primary entry
      *
      * @param parentFactory Factory used for parsing the primary (top-level) entries
      * @param midFactory Factory used for parsing the secondary (mid-level) entries
      * @param endFactory Factory used for parsing the tertiary (bottom-level) entries
      * @param mergeBottom Function that joins the bottom-level entries to their associated mid-level entry
      * @param mergeTop Function that joins the 'mergeBottom' merge results with their associated top-level entry
      *
      * @tparam P Type of the primary level entries
      * @tparam M Type of the secondary level entries
      * @tparam E Type of the tertiary level entries
      * @tparam RM Type of the lower merge results
      * @tparam R Type of the upper merge results
      * @return Upper merge results
      */
    def deepGroup[P, M, E, RM, R](parentFactory: FromRowFactory[P], midFactory: FromRowFactory[M],
                                  endFactory: FromRowFactory[E])
                                 (mergeBottom: (M, Vector[E]) => RM)
                                 (mergeTop: (P, Iterable[RM]) => R) =
    {
        val tp = parentFactory.table
        val tm = midFactory.table
        val te = endFactory.table
        // Groups the rows based on the parent index
        rows.filter { _.containsDataForTable(tp) }.groupBy { _.indexForTable(tp) }.flatMap { case (_, rows) =>
            // Parses one parent entry for each unique index
            parentFactory.tryParse(rows.head).map { parent =>
                // Groups the sub-rows based on mid table index
                val mids = rows.filter { _.containsDataForTable(tm) }.groupBy { _.indexForTable(tm) }
                    .flatMap { case (_, rows) =>
                        // Again, parses a single mid-entry for each unique index
                        midFactory.tryParse(rows.head).map { mid =>
                            // Finally, parses an entry for each remaining sub-row
                            val ends = rows.view
                                .filter { _.containsDataForTable(te) }
                                .distinctBy { _.indexForTable(te) }
                                .flatMap(endFactory.tryParse)
                                .toVector
                            // Joins the 3rd level entries to 2nd level entries
                            mergeBottom(mid, ends)
                        }
                    }
                // Joins the 2nd level merge results to 1st level entries
                mergeTop(parent, mids)
            }
        }
    }
    
    /**
      * Parses the rows using a factory, but leaves the post-processing open for the function-caller.
      * Assumes that all rows represent unique entities. If they don't please use [[groupAnd]] instead.
      * @param parentFactory Factory used for processing the primary entries
      * @param postProcess Function that continues the processing.
      *                    Accepts the parsed item, plus the row from which that item was parsed.
      * @tparam P Type of the parsed items
      * @tparam R Type of post-processed results
      * @return Post-processed results
      */
    def parseAnd[P, R](parentFactory: FromRowFactory[P])(postProcess: (P, Row) => R) =
        rows.view.zipFlatMap(parentFactory.parseIfPresent).map { case (row, parsed) => postProcess(parsed, row) }
            .toVector
    /**
      * Groups and parses the rows to those related to specific items.
      * Continues the per-item processing using the specified function.
      * @param parentFactory Factory used for parsing the primary items.
      * @param postProcess Function that accepts the primary item, plus the associated rows,
      *                    and returns a processed item.
      * @tparam P Type of the primary items
      * @tparam R Type of the post-processed results
      * @return Post-processed results
      */
    def groupAnd[P, R](parentFactory: FromRowFactory[P])(postProcess: (P, Seq[Row]) => R) = {
        val table = parentFactory.table
        // Groups the rows based on unique indices
        rows.filter { _.containsDataForTable(table) }.groupBy { _.indexForTable(table) }.flatMap { case (_, rows) =>
            // Parses one entry for each unique index
            parentFactory.tryParse(rows.head).map { parent =>
                // Continues processing with the specified function
                postProcess(parent, rows)
            }
        }
    }
    /**
      * Groups and parses the rows based on three levels of items.
      * The first and secondary levels are processed using the specified factories,
      * but the third level is delegated to the specified function.
      *
      * Post-processed secondary results are merged into groups based on the primary parse results.
      *
      * @param parentFactory Factory used for parsing the top-level items
      * @param midFactory Factory used for parsing the mid-level items
      * @param postProcessMid Function that accepts a mid-level item, as well as the associated rows,
      *                       and returns a post-processed result.
      * @param mergeTop Function that joins the post-processed results to their associated primary item.
      *
      * @tparam P Type of the primary items
      * @tparam M Type of the secondary items
      * @tparam RM Type of the lower level post-processed results
      * @tparam R Type of the joined results
      *
      * @return Joined results
      */
    def deepGroupAnd[P, M, RM, R](parentFactory: FromRowFactory[P], midFactory: FromRowFactory[M])
                                 (postProcessMid: (M, Seq[Row]) => RM)
                                 (mergeTop: (P, Iterable[RM]) => R) =
    {
        val tp = parentFactory.table
        val tm = midFactory.table
        // Groups the rows based on primary table index
        rows.filter { _.containsDataForTable(tp) }.groupBy { _.indexForTable(tp) }.flatMap { case (_, rows) =>
            // Parses one entry for each unique index
            parentFactory.tryParse(rows.head).map { parent =>
                // Groups the sub-rows based on the mid-table index
                val midResults = rows.filter { _.containsDataForTable(tm) }.groupBy { _.indexForTable(tm) }
                    .flatMap { case (_, rows) =>
                        // Again, parses one entry per index
                        // Processes these entries using the specified function
                        midFactory.tryParse(rows.head).map { mid => postProcessMid(mid, rows) }
                    }
                // Merges the results
                mergeTop(parent, midResults)
            }
        }
    }
    
    /**
     * Divides this result into multiple sub-results based on a table id
     * @param primaryTable The table based on which the this result is split
     * @return Sub-results found. Please note that this won't include any results / rows without data from the primary table.
     */
    def split(primaryTable: Table) = {
        val rowsPerId = rows.filter { _.containsDataForTable(primaryTable) }.groupBy { _.indexForTable(primaryTable) }
        rowsPerId.valuesIterator.map { rows => copy(rows = rows) }.toVector
    }
}