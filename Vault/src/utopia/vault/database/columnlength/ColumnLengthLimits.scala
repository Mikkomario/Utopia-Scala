package utopia.vault.database.columnlength

import utopia.flow.collection.immutable.{DeepMap, Pair}
import utopia.flow.view.mutable.async.Volatile
import utopia.vault.model.immutable.{Column, Table}

/**
  * Used for tracking length limits applying to each database column
  * @author Mikko Hilpinen
  * @since 25.12.2021, v1.12
  */
object ColumnLengthLimits
{
	// ATTRIBUTES   ------------------------------
	
	// (database name -> table name -> property name) => column length limit
	private val limits = Volatile(DeepMap.empty[String, ColumnLengthLimit])
	
	
	// IMPLEMENTED  ------------------------------
	
	override def toString = limits.value.toString
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param databaseName Name of targeted database
	  * @param tableName Name of targeted table
	  * @param propertyName Name of targeted property (column.name)
	  * @return A length limit to apply to that column, if specified
	  */
	def apply(databaseName: String, tableName: String, propertyName: String) =
		limits.value.get(databaseName, tableName, propertyName)
	/**
	  * @param databaseName Name of targeted database
	  * @param column Targeted column
	  * @return A length limit to apply to that column, if specified
	  */
	def apply(databaseName: String, column: Column): Option[ColumnLengthLimit] =
		apply(databaseName, column.tableName, column.name)
	/**
	  * @param table Targeted table
	  * @param propertyName Name of the targeted property within that table
	  * @return A length limit to apply to that column / property, if applicable
	  */
	def apply(table: Table, propertyName: String): Option[ColumnLengthLimit] =
		apply(table.databaseName, table.name, propertyName)
	/**
	  * @param databaseName Name of targeted database
	  * @param tableName Name of targeted table
	  * @return A map that contains length limits for property names where applicable
	  */
	def apply(databaseName: String, tableName: String) =
		limits.value.nested(databaseName, tableName).flat
	/**
	  * @param table Targeted table
	  * @return Column length limits to apply to that table
	  */
	def apply(table: Table): Map[String, ColumnLengthLimit] = apply(table.databaseName, table.name)
	
	/**
	  * @param key A key consisting of database name, table name and property name
	  * @param limit A limit to apply to that column
	  */
	def update(key: (String, String, String), limit: ColumnLengthLimit) =
		limits.update { _ + (Vector(key._1, key._2, key._3) -> limit) }
	/**
	  * @param databaseName Name of targeted database
	  * @param tableName Name of targeted table
	  * @param lengthLimits Length limits to apply, where keys are property names and values are length limits
	  */
	def update(databaseName: String, tableName: String, lengthLimits: IterableOnce[(String, ColumnLengthLimit)]) =
		limits.update { _ ++ (Pair(databaseName, tableName) -> DeepMap.flat(lengthLimits)) }
}
