package utopia.vault.database

import utopia.flow.generic.AnyType
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.Regex
import utopia.vault.database.columnlength.ColumnLengthLimits
import utopia.vault.model.immutable.{Column, Table}

/**
 * This object is able to read table / column data from the database itself
 * @author Mikko Hilpinen
 * @since 4.6.2017
 */
object DatabaseTableReader
{
    private lazy val splitterRegex = Regex.escape('_') || Regex.whiteSpace
    
    /**
     * Reads table data from the database
     * @param databaseName the name of the database the table is read from
     * @param tableName the name of the table that is read
     * @param connection the database connection that is used
     * @param columnNameToPropertyName a function that maps column names to property names. By 
     * default, converts all names with underscores and / or whitespaces to camel case 
     * (Eg. day_of_birth => dayOfBirth)
     */
    def apply(databaseName: String, tableName: String, 
            columnNameToPropertyName: String => String = underlineToCamelCase)(implicit connection: Connection) = 
    {
        // Reads the column data from the database
        connection.dbName = databaseName
        val columnData = connection.executeQuery(s"DESCRIBE `$tableName`")
        // [Column -> Option[LengthLimit]]
        val readColumns = columnData.map { data =>
            val columnName = data.getOrElse("COLUMN_NAME", data("Field"))
            val isPrimary = "pri" == data.getOrElse("COLUMN_KEY", data("Key")).toLowerCase
            val usesAutoIncrement = "auto_increment" == data.getOrElse("EXTRA", data("Extra")).toLowerCase
            val (foundType, lengthLimit) = SqlTypeInterpreterManager(data.getOrElse("COLUMN_TYPE", data("Type")))
            val dataType = foundType.getOrElse(AnyType)
            val nullAllowed = "yes" == data.getOrElse("IS_NULLABLE", data("Null")).toLowerCase
            
            val defaultString = data.getOrElse("COLUMN_DEFAULT", data.getOrElse("Default", "null"))
            val defaultValue = if (defaultString.toLowerCase == "null") None else defaultString.castTo(dataType)
            // Used to have:  || defaultString.toLowerCase == "current_timestamp"
            
            Column(columnNameToPropertyName(columnName), columnName, tableName, dataType,
                defaultValue, nullAllowed, isPrimary, usesAutoIncrement) -> lengthLimit
        }
        // Registers read column length limits
        ColumnLengthLimits.update(databaseName, tableName,
            readColumns.flatMap { case (col, limit) => limit.map { col.columnName -> _ } })
        
        Table(tableName, databaseName, readColumns.map { _._1 })
    }
    
    // Converts underscrore naming style strings to camelcase naming style strings
    // Eg. day_of_birth => dayOfBirth
    def underlineToCamelCase(original: String) =
    {
        // whitespaces are considered equal to underscrores, in case someone would use them
        val splits = splitterRegex.split(original)
        splits.tail.foldLeft(splits.head) { _ + _.capitalize }
    }
}