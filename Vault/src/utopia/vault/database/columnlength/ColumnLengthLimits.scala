package utopia.vault.database.columnlength

import utopia.flow.async.Volatile
import utopia.flow.datastructure.immutable.DeepMap

/**
  * Used for tracking length limits applying to each database column
  * @author Mikko Hilpinen
  * @since 25.12.2021, v1.12
  */
object ColumnLengthLimits
{
	// ATTRIBUTES   ------------------------------
	
	// (database name -> table name -> property name) => column length limit
	private val limits2 = Volatile(DeepMap.empty[String, ColumnLengthLimit])
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param databaseName Name of targeted database
	  * @param tableName Name of targeted table
	  * @param propertyName Name of targeted property (column.name)
	  * @return A length limit to apply to that column, if specified
	  */
	def apply(databaseName: String, tableName: String, propertyName: String) =
		limits2.value.get(databaseName, tableName, propertyName)
	/**
	  * @param key A key consisting of database name, table name and property name
	  * @param limit A limit to apply to that column
	  */
	def update(key: (String, String, String), limit: ColumnLengthLimit) =
		limits2.update { _ + (Vector(key._1, key._2, key._3) -> limit) }
	/**
	  * @param databaseName Name of targeted database
	  * @param tableName Name of targeted table
	  * @param lengthLimits Length limits to apply, where keys are property names and values are length limits
	  */
	def update(databaseName: String, tableName: String, lengthLimits: IterableOnce[(String, ColumnLengthLimit)]) =
		limits2.update { _ ++ (Vector(databaseName, tableName) -> DeepMap.flat(lengthLimits)) }
}
