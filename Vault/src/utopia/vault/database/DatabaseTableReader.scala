package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.AnyType
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.vault.database.columnlength.ColumnLengthLimits
import utopia.vault.model.immutable.{Column, Table}

/**
 * This object is able to read table / column data from the database itself
 * @author Mikko Hilpinen
 * @since 4.6.2017
 */
object DatabaseTableReader
{
    // ATTRIBUTES   --------------------------
    
    private lazy val splitterRegex = Regex.escape('_') || Regex.whiteSpace || Regex.escape('-')
    
    
    // OTHER    -----------------------------
    
    /**
     * Reads table data from the database
     * @param databaseName the name of the database the table is read from
     * @param tableName the name of the table that is read
     * @param connection the database connection that is used
     * @param columnNamesToPropertyNames A function that accepts all column names in a single table and
      *                                   yields a map that converts column names to (db) property names.
      *                                   The default function extracts a common prefix from the columns, if present,
      *                                   and converts the remaining part from underscore or kebab-case naming style
      *                                   to camel-case naming style.
      *
      *                                   E.g.
      *                                   - "abc_column_name" would be converted to "columnName"
      *                                     (if "abc_" was a common prefix in this table),
      *                                   - "prop_name" would convert to "propName",
      *                                   - "propName" would convert to "propName",
      *                                   - "abc_columnName" would convert to "columnName" (assuming prefix),
      *                                   - "my-prop-name" would convert to "myPropName"
     */
    def apply(databaseName: String, tableName: String,
              columnNamesToPropertyNames: Iterable[String] => Map[String, String] = columnNamesToPropertyNames)
             (implicit connection: Connection) =
    {
        // Reads the column data from the database
        connection.dbName = databaseName
        // Maps each column to its name
        val columnData = connection.executeQuery(s"DESCRIBE `$tableName`")
            .toMapBy { data => data.getOrElse("COLUMN_NAME", data("Field")) }
        // Determines column property names
        val colPropNames = columnNamesToPropertyNames(columnData.keys)
        // [Column -> Option[LengthLimit]]
        val readColumns = columnData.map { case (columnName, data) =>
            val isPrimary = "pri" == data.getOrElse("COLUMN_KEY", data("Key")).toLowerCase
            val usesAutoIncrement = "auto_increment" == data.getOrElse("EXTRA", data("Extra")).toLowerCase
            val (foundType, lengthLimit) = SqlTypeInterpreterManager(data.getOrElse("COLUMN_TYPE", data("Type")))
            val dataType = foundType.getOrElse(AnyType)
            val nullAllowed = "yes" == data.getOrElse("IS_NULLABLE", data("Null")).toLowerCase
            
            val defaultString = data.getOrElse("COLUMN_DEFAULT", data.getOrElse("Default", "null"))
            val defaultValue = if (defaultString.toLowerCase == "null") None else defaultString.castTo(dataType)
            // Used to have:  || defaultString.toLowerCase == "current_timestamp"
            
            Column(colPropNames.getOrElse(columnName, columnName), columnName, tableName, dataType,
                defaultValue, nullAllowed, isPrimary, usesAutoIncrement) -> lengthLimit
        }
        // Registers read column length limits
        ColumnLengthLimits.update(databaseName, tableName,
            readColumns.flatMap { case (col, limit) => limit.map { col.name -> _ } })
        
        Table(tableName, databaseName, readColumns.keys.toVector)
    }
    
    /**
      * Converts underscore naming style strings to camelcase naming style strings
      * Eg. "day_of_birth" => "dayOfBirth".
      * Also converts from kebab-case and whitespace-separated words.
      * @param original The original string
      * @return A camel-case version of that string
      */
    def underscoreToCamelCase(original: String) = {
        // Whitespaces and dashes are considered same as underscores, in case someone would use them
        val parts = splitterRegex.split(original)
        s"${parts.head}${parts.tail.map { _.capitalize }.mkString}"
    }
    // Converts underscore naming style strings to camelcase naming style strings
    // Eg. day_of_birth => dayOfBirth
    @deprecated("Renamed to .underscoreToCamelCase(String)", "1.17")
    def underlineToCamelCase(original: String) = underscoreToCamelCase(original)
    
    /**
      * The default implementation of the "column names to (db) property names" -function used in table reading.
      * This implementation extracts a common prefix from column names and converts the remaining part to camel-case.
      * This function supports underscore, camel-case and kebab-case naming style,
      * as well as whitespace -separated naming style
      * @param columnNames Column names in a table
      * @return A map that contains a database property name for each presented column name
      */
    def columnNamesToPropertyNames(columnNames: Iterable[String]) = {
        // Checks whether a common prefix is applied to the columns
        val prefix = {
            // The prefix may be in underscore, kebab-case, camel-case or none of these
            val prefixes = {
                if (columnNames.forall { _.contains('_') })
                    columnNames.map { _.takeTo { _ == '_' } }.toSet
                else if (columnNames.forall { _.contains('-') })
                    columnNames.map { _.takeTo { _ == '-' } }.toSet
                else if (columnNames.forall { _.exists { _.isUpper } })
                    columnNames.map { _.takeWhile { _.isLower } }.toSet
                else
                    Set.empty[String]
            }
            prefixes.only
        }
        // Converts the remaining part to camel-case
        prefix match {
            case Some(prefix) =>
                val prefixLength = prefix.length
                columnNames.mapTo { n => underscoreToCamelCase(n.drop(prefixLength)).uncapitalize }
            case None => columnNames.mapTo(underscoreToCamelCase)
        }
    }
}