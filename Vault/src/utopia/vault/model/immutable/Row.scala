package utopia.vault.model.immutable

import utopia.flow.generic.model.immutable.{Model, Value}

/**
 * A row represents a single row in a query result set. A row can contain columns from multiple
 * tables, if it is generated from a join query
 * @author Mikko Hilpinen
 * @since 30.4.2017
 * @param columnData Column data contains models generated on retrieved columns. There's a separate
 * model for each table. The table's name is used as a key in this map.
  * @param otherData Data outside of any included table in model format. Default = empty model.
 */
case class Row(columnData: Map[Table, Model], otherData: Model = Model.empty)
{
    // ATTRIBUTES   ---------------------------
    
    /**
      * This row represented as a single model. If there are results from multiple tables in this
      * row, this model may not contain all of that data because of duplicate attribute names.
      */
    lazy val toModel = if (columnData.nonEmpty) otherData ++ columnData.values.reduce { _ ++ _ } else otherData
    
    
    // COMPUTED PROPERTIES    -----------------
    
    /**
      * @return Whether this row contains data besides table-specific data
      */
    def containsOtherData = otherData.hasNonEmptyValues
    
    /**
     * Whether this row is empty and contains no column value data at all
     */
    def isEmpty = columnData.values.forall { _.hasOnlyEmptyValues } && otherData.hasOnlyEmptyValues
    
    /**
     * The indices for each of the contained table
     */
    def indices = columnData.flatMap { case (table, model) => table.primaryColumn.flatMap { column =>
            model.findExisting(column.name).map { constant => (table, constant.value) } } }
    
    /**
      * @return An index from this row. If this row contains data from multiple tables, please use index(Table) or
      *         indices instead.
      */
    def index = indices.headOption.map { _._2 } getOrElse Value.empty
    
    /**
      * @return The first value found from this row. Should only be used when just a single value is requested
      */
    def value = columnData.values.find { _.hasNonEmptyValues }.flatMap {
        _.attributesWithValue.headOption.map { _.value } } orElse
        otherData.attributesWithValue.headOption.map { _.value } getOrElse Value.empty
    
    
    // IMPLEMENTED  ---------------------------
    
    override def toString =
    {
        val tableString = columnData.toVector.map { case (table, data) => s"${table.name}: $data" }.mkString(", ")
        if (containsOtherData)
        {
            if (tableString.nonEmpty)
                s"[$tableString, other: $otherData]"
            else
                otherData.toString
        }
        else
            s"[$tableString]"
    }
    
    
    // OPERATORS    ---------------------------
    
    /**
     * Finds column data for a table
     * @param table The table whose data is requested
     */
    def apply(table: Table) = columnData.getOrElse(table, Model.empty)
    
    /**
     * Finds the value of a single property in the row
     * @param propertyName the name of the property
     */
    def apply(propertyName: String) = toModel(propertyName)
    
    /**
      * @param column Target column
      * @return The value for the specified column
      */
    def apply(column: Column) = columnData.find { _._1.contains(column) }.map { _._2(column.name) }
        .getOrElse(Value.emptyWithType(column.dataType))
    
    
    // OTHER METHODS    ----------------------
    
    /**
     * Finds the index of the row in the specified table
     */
    def indexForTable(table: Table) = table.primaryColumn.map { column => apply(table)(column.name) }
        .getOrElse(Value.empty)
    
    /**
      * Checks whether this row contains any data for the specified table
      * @param table Targeted table
      * @return Whether this row contains any data for that table
      */
    def containsDataForTable(table: Table) = columnData.get(table).exists { _.hasNonEmptyValues }
}
