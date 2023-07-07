package utopia.vault.database

import scala.concurrent.ExecutionContext

/**
  * Keeps track & setups of all database tables and their references by reading data directly from the database.
  * @author Mikko Hilpinen
  * @since 13.7.2019, v1.3
  */
class Tables(connectionPool: ConnectionPool)(implicit exc: ExecutionContext)
{
	// ATTRIBUTES	---------------------
	
	private var dbs = Map[String, TablesReader]()
	private var _columnNameConversion: Iterable[String] => Map[String, String] =
		DatabaseTableReader.columnNamesToPropertyNames
	
	
	// COMPUTED	-------------------------
	
	/**
	 * @return The method used when converting column names in the database to column attribute names in code (by default converts
	 *         from underscore style to camel case style, eg. "column_name" to "columnName")
	 */
	def columnNameConversion = _columnNameConversion
	/**
	 * Specifies a new name conversion style for tables read from the DB
	 * @param newConversionMethod A new method for column name conversion. Takes the database-originated column name
	 *                            as parameter and returns the column attribute name used in the code.
	 */
	def columnNameConversion_=(newConversionMethod: Iterable[String] => Map[String, String]) =
	{
		// Has to clear all existing data to use the new method
		_columnNameConversion = newConversionMethod
		dbs.keys.foreach(References.clear)
		dbs = Map()
	}
	
	
	// OPERATORS	---------------------
	
	/**
	  * Finds data for a specific table. Initializes database table data if necessary.
	  * @param dbName Database name
	  * @param tableName Table name
	  * @return That table's data
	  */
	def apply(dbName: String, tableName: String) = reader(dbName)(tableName.toLowerCase)
	
	
	// OTHER	-----------------------
	
	/**
	  * @param dbName Targeted database name
	  * @return All tables in the specified database
	  */
	def all(dbName: String) = reader(dbName).tables.values
	
	private def reader(dbName: String) = {
		val lowerDbName = dbName.toLowerCase
		if (dbs.contains(lowerDbName))
			dbs(lowerDbName)
		else {
			// May have to initialize new databases
			val db = new TablesReader(dbName)
			dbs += lowerDbName -> db
			db
		}
	}
	
	
	// NESTED	----------------------
	
	private class TablesReader(val dbName: String)
	{
		// ATTRIBUTES	-------------------
		
		val tables = {
			connectionPool { implicit connection =>
				connection.dbName = dbName
				// First finds out table names using "show tables"
				val tableNames = connection.executeQuery("show tables").flatMap { _.values.headOption }
				// Reads data for each table
				val tables = tableNames.map { DatabaseTableReader(dbName, _, columnNameConversion) }
				// Sets up references between the tables
				DatabaseReferenceReader.setupReferences(tables.toSet)
				
				tables.map { t => t.name.toLowerCase -> t }.toMap
			}
		}
		
		
		// OTHER	-------------------
		
		def apply(tableName: String) = {
			if (tables.contains(tableName))
				tables(tableName)
			else
				throw new NoSuchTableException(dbName, tableName)
		}
	}
	
	private class NoSuchTableException(dbName: String, tableName: String) extends RuntimeException(
		s"Database $dbName doesn't contain a table named $tableName")
}