package utopia.vault.database.columnlength

import utopia.flow.datastructure.immutable.DeepMap
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.JsonParser
import utopia.flow.util.StringExtensions._
import utopia.vault.database.ConnectionPool
import utopia.vault.database.columnlength.ColumnLengthRule.{Throw, TryCrop, TryExpand}

import java.nio.file.Path
import scala.concurrent.ExecutionContext

/**
  * An object used for managing cases where column maximum length is exceeded, as there are many ways to proceed
  * in such cases.
  * @author Mikko Hilpinen
  * @since 21.11.2021, v1.12
  */
object ColumnLengthRules
{
	// ATTRIBUTES   --------------------------------
	
	/**
	  * The column length rule applied by default (default = throw error if column maximum length is exceeded)
	  */
	var default = Throw
	
	// Key path is: database name -> table name -> property name
	private var specifics = DeepMap.empty[String, ColumnLengthRule]
	
	
	// OTHER    ------------------------------------
	
	/**
	  * @param databaseName Name of the targeted database
	  * @param tableName Name of the targeted table
	  * @param propertyName Name of the targeted property (based on column name)
	  * @return A column length rule to apply to that column
	  */
	def apply(databaseName: String, tableName: String, propertyName: String) =
		specifics.get(databaseName, tableName, propertyName).getOrElse(default)
	
	/**
	  * Applies a new rule to a column
	  * @param databaseName Name of the targeted database
	  * @param tableName Name of the targeted table
	  * @param propertyName Name of the targeted property (based on column name)
	  * @param rule Rule to apply to that column
	  */
	def update(databaseName: String, tableName: String, propertyName: String, rule: ColumnLengthRule) =
		specifics += (Vector(databaseName, tableName, propertyName) -> rule)
	
	/**
	  * Loads column length rules from a .json file. The file should contain a json object with database names as
	  * property names and objects as values. The nested objects should contain tables in a similar fashion, which
	  * then again should contain column properties. The column properties should have values such as "throw", "crop",
	  * "expand" or "expand to X" where X is the maximum allowed length.
	  * @param path Path to the json file to read
	  * @param jsonParser Json parser to use
	  * @param exc Implicit execution context (used when handling connections opened during rule handling)
	  * @param connectionPool Implicit connection pool (used for expanding column lengths when needed)
	  * @return Success or failure, based on json parse result
	  */
	def loadFrom(path: Path)(implicit jsonParser: JsonParser, exc: ExecutionContext, connectionPool: ConnectionPool) =
		jsonParser(path).map { json =>
			val limits = json.getModel.attributes.flatMap { dbAtt =>
				val dbName = dbAtt.name
				dbAtt.value.getModel.attributes.flatMap { tableAtt =>
					val tableName = tableAtt.name
					tableAtt.value.getModel.attributes.flatMap { columnAtt =>
						val propName = columnAtt.name
						val value = columnAtt.value.getString.toLowerCase
						val limit = value match {
							case "throw" => Some(Throw)
							case "crop" => Some(TryCrop)
							case "expand" => Some(TryExpand.infinitely)
							case _ =>
								if (value.startsWith("expand") || value.startsWith("to"))
									value.afterLast(" ").long.map { TryExpand upTo _ }
								else
									None
						}
						limit.map { Vector(dbName, tableName, propName) -> _ }
					}
				}
			}
			specifics ++= DeepMap(limits)
		}
}
