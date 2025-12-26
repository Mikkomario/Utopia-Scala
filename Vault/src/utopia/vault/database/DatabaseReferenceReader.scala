package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.HasValues
import utopia.vault.model.immutable.{Column, Reference, Table}
import utopia.vault.sql.{ConditionElement, Select, Where}

/**
  * This object can be used for reading and setting up table references by reading them directly
  * from the database.
  * @author Mikko Hilpinen
  * @since 9.6.2017
  */
object DatabaseReferenceReader
{
	// ATTRIBUTES   -------------------------
	
	private val keys = Table("KEY_COLUMN_USAGE", "INFORMATION_SCHEMA", Vector(
		Column("schema", "TABLE_SCHEMA", "KEY_COLUMN_USAGE", StringType, allowsNull = false),
		Column("tableName", "TABLE_NAME", "KEY_COLUMN_USAGE", StringType),
		Column("columnName", "COLUMN_NAME", "KEY_COLUMN_USAGE", StringType),
		Column("referencedTableName", "REFERENCED_TABLE_NAME", "KEY_COLUMN_USAGE", StringType),
		Column("referencedColumnName", "REFERENCED_COLUMN_NAME", "KEY_COLUMN_USAGE", StringType)
	))
	
	
	// OTHER    ----------------------------
	
	/**
	  * Reads all references between the provided tables
	  * @param tables the tables for which the references are read
	  * @param connection the database connection that is used
	  * @return References between the specified tables
	  */
	def apply(tables: Iterable[Table])(implicit connection: Connection) =
		tables.groupToSeqsBy { _.databaseName }.flatMap { case (dbName, tables) => _apply(dbName, tables) }
	
	/**
	  * Sets up the References object to contain all references between the provided tables. If
	  * there are tables from multiple databases, references are set up for all of them.
	  * @param tables the tables between which the references are searched. Should contain all
	  * tables for each included database
	  * @param connection the database connection used
	  */
	def setupReferences(tables: Iterable[Table])(implicit connection: Connection) =
		tables.groupToSeqsBy { _.databaseName }.foreach { case (dbName, dbTables) =>
			References.setup(dbName, _apply(dbName, dbTables))
		}
	
	private def _apply(dbName: String, tables: Iterable[Table])(implicit connection: Connection) =
		if (tables.isEmpty)
			Empty
		else {
			val dbCondition = keys("schema") <=> dbName
			val condition = {
				if (tables.hasSize > 8)
					dbCondition
				else {
					val tableOptions = tables.map { _.name: ConditionElement }
					dbCondition && (keys("tableName").in(tableOptions), keys("referencedTableName").in(tableOptions))
				}
			}
			val results = connection(Select.all(keys) + Where(condition)).rows.map { _.toModel }
			
			def findTable(keyName: String, row: HasValues) =
				tables.find { _.name == row(keyName).getString }
			def findColumn(table: Table, keyName: String, row: HasValues) =
				table.findColumnWithName(row(keyName).getString)
			
			results.flatMap { row =>
				findTable("tableName", row).flatMap { sourceTable =>
					findTable("referencedTableName", row).flatMap { targetTable =>
						findColumn(sourceTable, "columnName", row).flatMap { sourceCol =>
							findColumn(targetTable, "referencedColumnName", row).map { targetCol =>
								Reference(sourceCol, targetCol)
							}
						}
					}
				}
			}
		}
}