package utopia.vault.database.columnlength

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.DeepMap
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.vault.database.ConnectionPool
import utopia.vault.database.columnlength.ColumnLengthRule.{Throw, TryCrop, TryExpand}
import utopia.vault.model.immutable.{Column, Table}

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
	
	private lazy val ruleSplitRegex = Regex.whiteSpace + Regex("or") + Regex.whiteSpace
	
	/**
	  * The column length rule applied by default (default = throw error if column maximum length is exceeded)
	  */
	var default = Throw
	
	// Key path is: database name -> table name -> property name
	private var specifics = DeepMap.empty[String, ColumnLengthRule]
	
	
	// IMPLEMENTED  --------------------------------
	
	override def toString = specifics.toString
	
	
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
	  * @param databaseName Name of targeted database
	  * @param column Targeted column
	  * @return A length rule to apply to that column, if specified
	  */
	def apply(databaseName: String, column: Column): ColumnLengthRule =
		apply(databaseName, column.tableName, column.name)
	/**
	  * @param table Targeted table
	  * @param propertyName Name of the targeted property within that table
	  * @return A length rule to apply to that column / property, if applicable
	  */
	def apply(table: Table, propertyName: String): ColumnLengthRule =
		apply(table.databaseName, table.name, propertyName)
	
	/**
	  * @param databaseName Name of the targeted database
	  * @param tableName name of the targeted table
	  * @return A map containing all specific column length rules and including the default column length rule
	  */
	def apply(databaseName: String, tableName: String) =
		specifics.nested(databaseName, tableName).flat.withDefault { _ => default }
	/**
	  * @param table Targeted table
	  * @return Column length rules to apply to that table
	  */
	def apply(table: Table): Map[String, ColumnLengthRule] = apply(table.databaseName, table.name)
	
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
			val limits = json.getModel.properties.flatMap { dbAtt =>
				loadFromDbModel(dbAtt.name, dbAtt.value.getModel)
			}
			specifics ++= DeepMap(limits)
		}
	
	/**
	  * Loads column length rules from a .json file. The file should contain a json object with table names as
	  * property names and objects as values. The nested objects should contain column properties.
	  * The column properties should have values such as "throw", "crop",
	  * "expand" or "expand to X" where X is the maximum allowed length.
	  * @param path Path to the json file to read
	  * @param databaseName Name of the database to which to apply these rules.
	 *                     Call-by-name; Not called if the file itself specifies database names.
	  * @param jsonParser Json parser to use
	  * @param exc Implicit execution context (used when handling connections opened during rule handling)
	  * @param connectionPool Implicit connection pool (used for expanding column lengths when needed)
	  * @return Success or failure, based on json parse result
	  */
	def loadFrom(path: Path, databaseName: => String)
	            (implicit jsonParser: JsonParser, exc: ExecutionContext, connectionPool: ConnectionPool) =
	{
		jsonParser(path).map { json =>
			val model = json.getModel
			
			// Checks whether the file specifies a database name already
			val limits = {
				// Case: Database names are specified within the file => These overwrite the specified DB name
				if (model.propertiesIterator.nextOption().exists { _.value.model.isDefined })
					model.properties.flatMap { dbAtt => loadFromDbModel(dbAtt.name, dbAtt.value.getModel) }
				// Case: Database names not specified => Continues with the specified database name
				else
					loadFromDbModel(databaseName, json.getModel)
			}
			specifics ++= DeepMap(limits)
		}
	}
	
	private def loadFromDbModel(dbName: String, model: Model)
	                           (implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
	{
		// Handles each column in each table
		model.properties.flatMap { tableAtt =>
			val tableName = tableAtt.name
			tableAtt.value.getModel.properties.flatMap { columnAtt =>
				val propName = columnAtt.name
				// Multiple rules may be combined using "A or B" syntax where A and B are separate rules
				val parts = columnAtt.value.getString.toLowerCase.split(ruleSplitRegex).map { _.trim }
				// Parses each part
				val limits = parts.flatMap {
					case "throw" => Some(Throw)
					case "crop" => Some(TryCrop)
					case "expand" => Some(TryExpand.infinitely)
					case other =>
						if (other.startsWith("expand") || other.startsWith("to") || other.startsWith("up to"))
							other.afterLast(" ").long.map { TryExpand upTo _ }
						else
							other.long.map { TryExpand upTo _ }
				}
				// Combines the parts into a single limit
				val limit = limits.headOption.map { first => limits.tail.foldLeft(first) { _.recoverWith(_) } }
				limit.map { Vector(dbName, tableName, propName) -> _ }
			}
		}
	}
}
