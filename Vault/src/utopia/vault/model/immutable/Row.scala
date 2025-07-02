package utopia.vault.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.operator.MaybeEmpty

object Row
{
	/**
	  * An empty row
	  */
	lazy val empty = apply(Map())
}

/**
  * Represents a single database row pulled with a select query
  * @author Mikko Hilpinen
  * @since 30.4.2017
  * @param tableModels (Database) models formed based on read data, grouped by table name.
  *                    Model properties are named based on pulled column property names.
  * @param other A model containing all data outside the included tables.
  *                  Default = empty model.
  */
case class Row(tableModels: Map[String, Model], other: Model = Model.empty) extends MaybeEmpty[Row]
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * This row represented as a single model. If there are results from multiple tables in this
	  * row, this model may not contain all of that data because of duplicate property names.
	  */
	lazy val toModel = {
		if (tableModels.isEmpty)
			other
		else if (other.isEmpty)
			tableModels.valuesIterator.reduce { _ ++ _ }
		else
			tableModels.valuesIterator.foldLeft(other) { _ ++ _ }
	}
	
	
	// COMPUTED PROPERTIES    -----------------
	
	/**
	  * @return Whether this row contains data besides table-specific data
	  */
	def containsOtherData = other.hasNonEmptyValues
	
	/**
	  * @return The first value found from this row. Should only be used when just a single value is requested
	  */
	def value =
		tableModels.valuesIterator.flatMap { _.propertiesIterator.map { _.value }.filter { _.nonEmpty } }.nextOption()
			.getOrElse { other.propertiesIterator.map { _.value }.find { _.nonEmpty }.getOrElse(Value.empty) }
	
	@deprecated("Renamed to .other", "v1.22")
	def otherData = other
	
	
	// IMPLEMENTED  ---------------------------
	
	override def self: Row = this
	
	/**
	  * Whether this row is empty and contains no column value data at all
	  */
	override def isEmpty = tableModels.valuesIterator.forall { _.hasOnlyEmptyValues } && other.hasOnlyEmptyValues
	
	override def toString = {
		val models = {
			if (other.hasNonEmptyValues)
				tableModels + ("other" -> other)
			else
				tableModels
		}
		models.emptyOneOrMany match {
			case None => "{}"
			case Some(Left((_, model))) => model.toString
			case Some(Right(models)) =>
				s"${ models.iterator.map { case (table, model) => s"\"$table\": $model" }.mkString(", ") }"
		}
	}
	
	
	// OPERATORS    ---------------------------
	
	/**
	  * Finds column data for a table
	  * @param table The table whose data is requested
	  */
	def apply(table: Table) = this.table(table.name)
	/**
	  * @param tableName Name of the targeted table
	  * @return A model representing that table's collected data
	  */
	def table(tableName: String) = tableModels.getOrElse(tableName, Model.empty)
	
	/**
	  * Finds the value of a single property in the row
	  * @param propertyName the name of the property
	  */
	def apply(propertyName: String) = toModel(propertyName)
	/**
	  * @param tableName Name of the targeted table
	  * @param propertyName Name of the targeted property
	  * @return Value of the targeted property
	  */
	def apply(tableName: String, propertyName: String) = tableModels.get(tableName) match {
		case Some(table) => table(propertyName)
		case None => Value.empty
	}
	/**
	  * @param column Targeted column
	  * @return Value of that column
	  */
	def apply(column: Column): Value = apply(column.tableName, column.name)
	/**
	  * @param column Targeted column
	  * @return Value of that column
	  */
	def apply(column: TableColumn): Value = apply(column.column)
	
	
	// OTHER METHODS    ----------------------
	
	/**
	  * Finds the index of the row in the specified table
	  */
	def indexForTable(table: Table) = table.primaryColumn match {
		case Some(col) => apply(col)
		case None => Value.empty
	}
	
	/**
	  * Checks whether this row contains any data for the specified table
	  * @param table Targeted table
	  * @return Whether this row contains any data for that table
	  */
	def containsDataForTable(table: Table): Boolean = containsDataForTable(table.name)
	/**
	  * Checks whether this row contains any data for the specified table
	  * @param table Targeted table
	  * @return Whether this row contains any data for that table
	  */
	def containsDataForTable(table: String) = tableModels.get(table).exists { _.hasNonEmptyValues }
}
