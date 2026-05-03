package utopia.vault.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.operator.MaybeEmpty
import utopia.vault.error.HandleError

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
  * @param models (Database) models formed based on read data, grouped by table name.
  *               Model properties are named based on pulled column property names.
  */
case class Row(models: Map[String, Model]) extends MaybeEmpty[Row]
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * This row represented as a single model. If there are results from multiple tables in this
	  * row, this model may not contain all of that data because of duplicate property names.
	  */
	lazy val toModel = models.valuesIterator.reduceOption { _ ++ _ }.getOrElse(Model.empty)
	
	
	// COMPUTED PROPERTIES    -----------------
	
	/**
	  * @return The first value found from this row. Should only be used when just a single value is requested
	  */
	def value = models.valuesIterator.findMap { _.propertiesIterator.map { _.value }.find { _.nonEmpty } }
		.getOrElse(Value.empty)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def self: Row = this
	
	/**
	  * Whether this row is empty and contains no column value data at all
	  */
	override def isEmpty = models.valuesIterator.forall { _.hasOnlyEmptyValues }
	
	override def toString = {
		models.emptyOneOrMany match {
			case None => "{}"
			case Some(Left((_, model))) => model.toString
			case Some(Right(models)) =>
				s"${ models.iterator.map { case (table, model) => s"\"$table\": $model" }.mkString(", ") }"
		}
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Finds column data for a table
	  * @param table The table whose data is requested
	  */
	def apply(table: Table) = this.table(table.name)
	/**
	  * @param tableName Name of the targeted table
	  * @return A model representing that table's collected data
	  */
	def table(tableName: String) = models.getOrElse(tableName, Model.empty)
	
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
	def apply(tableName: String, propertyName: String) = models.get(tableName) match {
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
	/**
	 * @param prop Targeted database property
	 * @return Value of that property
	 */
	def apply(prop: DbPropertyDeclaration): Value = apply(prop.column)
	
	/**
	  * Finds the index of the row in the specified table
	  */
	def indexForTable(table: Table) = table.primaryColumn match {
		case Some(col) => apply(col)
		case None => Value.empty
	}
	
	/**
	 * @param column A column
	 * @return Whether this row contains data for that column
	 */
	def contains(column: Column) = models.get(column.tableName).exists { _.contains(column.name) }
	
	/**
	 * @param table Table to which an alias was given
	 * @param alias Alias that was given to the specified table
	 * @return A copy of this row mapping the aliased content back to the table name
	 */
	def applyingAlias(table: Table, alias: String) = models.get(alias) match {
		// Case: Alias found => Maps the column names into column DB property names
		case Some(aliased) =>
			val mapped = aliased.mapKeys { colName =>
				table.findColumnWithName(colName) match {
					case Some(col) => col.name
					case None => colName
				}
			}
			Row(models - alias + (table.name -> mapped))
		
		// Case: No alias found => Delegates to HandleError
		case None =>
			HandleError.inTableAliasing(new NoSuchElementException(s"No alias ´$alias´ was found"))
			Row(models - table.name)
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
	def containsDataForTable(table: String) = models.get(table).exists { _.hasNonEmptyValues }
}
