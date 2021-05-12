package utopia.vault.database

import java.nio.file.Path

import utopia.flow.generic.EnvironmentNotSetupException
import java.sql.DriverManager
import java.sql.Statement
import java.sql.SQLException

import utopia.flow.datastructure.immutable.Value
import java.sql.PreparedStatement

import utopia.flow.parse.ValueConverterManager
import java.sql.Types
import java.sql.ResultSet

import utopia.flow.datastructure.immutable.Model
import utopia.flow.datastructure.immutable.Constant

import scala.collection.immutable.HashSet
import scala.util.{Failure, Success, Try}
import utopia.flow.generic.IntType
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.AutoClose._
import utopia.flow.util.IterateLines
import utopia.vault.model.immutable.{Result, Row, Table}
import utopia.vault.sql.SqlSegment

object Connection
{
    // ATTRIBUTES    ------------------------
    
    /**
     * The converter that converts values to sql compatible format
     */
    val sqlValueConverter = new ValueConverterManager(Vector(BasicSqlValueConverter))
    /**
     * The generator that converts sql data (object + type) into a value
     */
    val sqlValueGenerator = new SqlValueGeneratorManager(Vector(BasicSqlValueGenerator))
    
    /**
     * The settings used for establishing new connections
     */
    var settings = ConnectionSettings()
    
    // If an external driver is used in database operations, it is stored here after instantiation
    private var driver: Option[Any] = None
    
    
    // OTHER METHODS    ---------------------
    
    /**
     * Creates a temporary database connection for a specific operation. The connection is closed 
     * after the operation completes, even in error situations. No errors are catched though
     * @param f The function that is performed and which uses a database connection
     */
    def doTransaction[T](f: Connection => T) = new Connection().consume(f)
    
    /**
     * Creates a temporary database connection for a specific operation. The connection is closed 
     * after the operation completes. Any errors are catched and the resulting try reflects the 
     * success / failure state of the operation
     */
    def tryTransaction[T](f: Connection => T) = Try { doTransaction(f) }
    
    /**
      * Modifies the settings used in database connections
      * @param mod A function that modifies settings
      */
    def modifySettings(mod: ConnectionSettings => ConnectionSettings) = settings = mod(settings)
}

/**
 * Instances of this class handle database connections and allow low level database interaction 
 * through SQL statements
 * @author Mikko Hilpinen
 * @since 16.4.2017
 */
class Connection(initialDBName: Option[String] = None) extends AutoCloseable
{
    // ATTRIBUTES    -----------------
    
    private var _dbName = initialDBName
    /**
     * The name of the database the connection is used for. This is either defined by<br>
     * a) specifying the database name upon connection creation<br>
     * b) specified after the connection has been instantiated by assigning a new value<br>
     * c) the default option specified in the connection settings
     */
    def dbName = _dbName.orElse(Connection.settings.defaultDBName)
    def dbName_=(databaseName: String) = 
    {
        if (!dbName.contains(databaseName))
        {
            // Performs a database change, if necessary
            if (isOpen && !_dbName.contains(databaseName))
                execute(s"USE $databaseName")
            
            _dbName = Some(databaseName)
        }
    }
    
    private var _connection: Option[java.sql.Connection] = None
    private def connection = { open(); _connection.get }
    
    
    // COMPUTED PROPERTIES    -------
    
    /**
     * Whether the connection to the database has already been established
     */
    def isOpen = _connection.exists { !_.isClosed }
    
    
    // OPERATORS    -----------------
    
    /**
     * Executes an sql statement and returns the results. The provided statement instance provides 
     * the exact parameters for the operation
     */
    def apply(statement: SqlSegment): Result = 
    {
        printIfDebugging("Executing statement: " + statement.description)
        val selectedTables: Set[Table] = if (statement.isSelect) statement.targetTables else HashSet()
        
        // Changes database if necessary
        statement.databaseName.foreach { dbName = _ }
        val result = apply(statement.sql, statement.values, selectedTables, statement.generatesKeys, statement.isSelect)
        
        printIfDebugging(s"Received result: $result")
        result
    }
    
    /**
     * Executes an sql query and returns the results. The provided values are injected to the 
     * query separately.
     * @param sql The sql string. Places for values are marked with '?'. There should always be 
     * exactly same number of these markers as there are values in the 'values' parameter
     * @param values The values that are injected to the query
     * @param selectedTables The tables for which resulting rows are parsed. If empty, the rows 
     * are not parsed at all.
     * @param returnGeneratedKeys Whether the resulting Result object should contain any keys 
     * generated during the query (default = false)
      * @param returnRows Whether rows should be returned from this query (default = true)
     * @return The results of the query, containing the read rows and keys. If 'selectedTables' 
     * parameter was empty, no rows are included. If 'returnGeneratedKeys' parameter was false, 
     * no keys are included. On update statements, includes number of updated rows.
      * @throws DBException If query failed for some reason
     */
    @throws(classOf[DBException])
    def apply(sql: String, values: Seq[Value], selectedTables: Set[Table] = HashSet(), 
            returnGeneratedKeys: Boolean = false, returnRows: Boolean = true) =
    {
        // Empty statements are not executed
        if (sql.isEmpty)
            Result.empty
        else 
        {
            Try
            {
                // Creates the statement
                connection.prepareStatement(sql,
                    if (returnGeneratedKeys) Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS).consume { statement =>
        
                    // Inserts provided values
                    setValues(statement, values)
        
                    // Executes the statement and retrieves the result (if available)
                    val foundResult = statement.execute()
                    // Case: Expecting generated keys
                    if (returnGeneratedKeys)
                        Result(Vector(), generatedKeysFromResult(statement, selectedTables))
                    else
                    {
                        // Collects the result rows or update count from the first result
                        var rows =
                        {
                            if (foundResult)
                                statement.getResultSet.consume { rowsFromResult(_, selectedTables) }
                            else
                                Vector()
                        }
                        var updateCount = if (foundResult) 0 else statement.getUpdateCount
                        // Handles possible additional results
                        var expectsMore = foundResult || updateCount >= 0
                        while (expectsMore)
                        {
                            // Case: Additional result with more rows
                            if (statement.getMoreResults)
                                rows ++= statement.getResultSet.consume { rowsFromResult(_, selectedTables) }
                            else
                            {
                                val newUpdateCount = statement.getUpdateCount
                                // Case: No more results
                                if (newUpdateCount < 0)
                                    expectsMore = false
                                // Case: Update count
                                else
                                    updateCount += newUpdateCount
                            }
                        }
                        Result(rows, updatedRowCount = updateCount max 0)
                    }
                }
            } match
            {
                case Success(result) => result
                case Failure(error) => throw new DBException(
                    s"DB query failed.\nSql: $sql\nValues:[${values.mkString(", ")}]", error)
            }
        }
    }
    
    
    // OTHER METHODS    -------------
    
    /**
     * Creates an iterator that splits the request to smaller queries.
     * This is useful when dealing with very large tables where memory usage becomes an issue.
     * Please note that the resulting iterator must be used only while this connection remains open and
     * must be discarded afterwards.
     * @param query The query performed on each iteration
     *                  (limit and offset will be applied in addition to this statement)
     * @param rowsPerIteration Number of rows returned on each iteration (default = defined by connection settings)
     * @return An iterator that returns a new result each time .next() is called.
     */
    def iterator(query: SqlSegment, rowsPerIteration: Int = Connection.settings.maximumAmountOfRowsCached) =
        new QueryIterator(query, rowsPerIteration)(this).filterNot { _.isEmpty }
    
    /**
     * Creates a row iterator that splits the request to smaller queries.
     * This is useful when dealing with very large tables where memory usage becomes an issue.
     * Please note that the resulting iterator must be used only while this connection remains open and
     * must be discarded afterwards.
     * @param query The query performed on each iteration
     *                  (limit and offset will be applied in addition to this statement)
     * @param rowsPerIteration Number of rows returned on each iteration (default = defined by connection settings)
     * @return A row iterator that performs the request(s) using limit and offset
     */
    def rowIterator(query: SqlSegment, rowsPerIteration: Int = Connection.settings.maximumAmountOfRowsCached) =
        new QueryIterator(query, rowsPerIteration)(this).flatMap { _.rows }
    
    /**
     * Performs an operation on each row targeted by specified statement
     * @param statement a (select) statement. <b>Must not include limit or offset</b>
     * @param operation Operation performed for each row
     */
    def foreach(statement: SqlSegment)(operation: Row => Unit) = rowIterator(statement).foreach(operation)
    
    /**
     * Maps read rows and reduces them into a single value. This should be used when handling queries which may
     * yield very large results.
     * @param statement A (select) statement. <b>Must not include limit or offset</b>
     * @param map A mapping function for rows
     * @param reduce A reduce function for mapped rows
     * @tparam A Type of map result
     * @return Reduction result. None if no data was read.
     */
    def mapReduce[A](statement: SqlSegment)(map: Row => A)(reduce: (A, A) => A) =
    {
        val resultIterator = iterator(statement)
        if (resultIterator.hasNext)
            Some(resultIterator.map { _.rows.map(map).reduce(reduce) }.reduce(reduce))
        else
            None
    }
    
    /**
     * Maps read rows and reduces them into a single value. This should be used when handling queries which may
     * yield very large results.
     * @param statement A (select) statement. <b>Must not include limit or offset</b>
     * @param map A mapping function for rows (may return 0 or multiple values)
     * @param reduce A reduce function for mapped rows
     * @tparam A Type of map result
     * @return Reduction result. None if no data was read.
     */
    def flatMapReduce[A](statement: SqlSegment)(map: Row => IterableOnce[A])(reduce: (A, A) => A) =
    {
        // Reduces the results as they arrive
        iterator(statement).map { _.rows.flatMap(map).reduceOption(reduce) }
            // Finally combines the reduce results
            .reduceOption { (a, b) =>
                a match
                {
                    case Some(definedA) =>
                        Some(b match
                        {
                            case Some(definedB) => reduce(definedA, definedB)
                            case None => definedA
                        })
                    case None => b
                }
            }.flatten
    }
    
    /**
     * Folds read rows into a single value. This function may prove useful with very large queries.
     * @param statement A (select) statement. <b>Must not include limit or offset</b>
     * @param start Starting value
     * @param f Folding function (takes previous result and adds one row to it)
     * @tparam A Result type
     * @return Fold result
     */
    def fold[A](statement: SqlSegment)(start: A)(f: (A, Row) => A) = rowIterator(statement).foldLeft(start)(f)
    
    /**
     * Tries to execute a statement. Wraps the results in a try
     */
    def tryExec(statement: SqlSegment) = Try(this(statement))
    
    /**
     * Opens a new database connection. This is done automatically when the connection is used, too
     */
    @throws(classOf[EnvironmentNotSetupException])
    @throws(classOf[NoConnectionException])
    def open() =
    {
        // Only opens a new connection if there is no open connection available
        if (!isOpen)
        {   
            // Database name must be specified at this point
            if (dbName.isEmpty)
                throw NoConnectionException("Database name hasn't been specified")
            
            Try
            {
                // Sets up the driver
                if (Connection.settings.driver.isDefined && Connection.driver.isEmpty)
                {
                    Connection.driver = Some(
                        Class.forName(Connection.settings.driver.get).newInstance())
                }
    
                // Instantiates the connection
                _connection = Some(DriverManager.getConnection(
                    Connection.settings.connectionTarget + dbName.get + Connection.settings.charsetString,
                    Connection.settings.user, Connection.settings.password))
            }.failure.foreach { e => throw NoConnectionException(
                s"Failed to open a database connection with settings ${Connection.settings} and database '$dbName'", e) }
        }
    }
    
    /**
     * Closes this database connection. This should be called before the connection is discarded
     */
    override def close() =
    {
        // Exceptions during closing are ignored
        Try { _connection.foreach { _.close() } }
        _connection = None
    }
    
    /**
     * Executes a simple sql string. Does not retrieve any values from the query.
     */
    @throws(classOf[EnvironmentNotSetupException])
    @throws(classOf[NoConnectionException])
    @throws(classOf[SQLException])
    def execute(sql: String): Unit =
    {
        // Empty statements are not executed
        if (sql.nonEmpty)
            connection.createStatement().consume { _.executeUpdate(sql) }
    }
    
    /**
     * Executes a query that allows use of prepared values. Reads and returns the resulting 
     * column data. Most of the time, using different versions of apply is better than using this method, 
     * but this one can be used without table data.
     * @param sql The sql string. Slots for values are indicated with question marks (?)
     * @param values the values inserted to the query. There should be a matching amount of values 
     * and slots in the sql string.
     * @return A map for each row. The map contains column name + column value pairs. Only non-null 
     * values are included.
     */
    @throws(classOf[EnvironmentNotSetupException])
    @throws(classOf[NoConnectionException])
    @throws(classOf[SQLException])
    def executeQuery(sql: String, values: Seq[Value] = Vector()) =
    {
        // Empty statements are not executed
        if (sql.isEmpty)
            Vector[Map[String, String]]()
        else 
        {
            // Creates the statement
            connection.prepareStatement(sql).consume { statement =>
                // Inserts provided values
                setValues(statement, values)
    
                // Executes the statement and retrieves the result
                statement.executeQuery().consume { results =>
                    val meta = results.getMetaData
    
                    val columnIndices = Vector.range(1, meta.getColumnCount + 1).map { index =>
                        (meta.getColumnName(index), index) }
    
                    // Parses data out of the result
                    val buffer = Vector.newBuilder[Map[String, String]]
                    while (results.next())
                    {
                        buffer += columnIndices.flatMap { case (name, index) =>
                            stringFromResult(results, index).map { (name, _) } }.toMap
                    }
    
                    buffer.result()
                }
            }
        }
    }
    
    /**
      * Checks whether a database with specified name exists
      * @param databaseName Database name
      * @return Whether such a database exists
      */
    def existsDatabaseWithName(databaseName: String) = executeQuery(
        "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ? LIMIT 1",
        Vector(databaseName)).nonEmpty
    
    /**
      * Checks whether there exists a database table combination
      * @param databaseName Database name
      * @param tableName Table name
      * @return Whether such a table exists in a database with that name
      */
    def existsTable(databaseName: String, tableName: String) = executeQuery(
        "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? LIMIT 1",
        Vector(databaseName, tableName)).nonEmpty
    
    /**
      * Creates a new database
      * @param databaseName Name of the new database
      * @param checkIfExists Whether database should only be created if one doesn't exist yet (default = true)
      * @param useNewDb Whether connection should switch to target the new database afterwards (default = true)
      */
    def createDatabase(databaseName: String, checkIfExists: Boolean = true, useNewDb: Boolean = true) =
    {
        execute(s"CREATE DATABASE${if (checkIfExists) " IF NOT EXISTS " else " "}$databaseName")
        if (useNewDb)
            dbName = databaseName
    }
    
    /**
      * Drops / removes a database
      * @param databaseName Database name
      * @param checkIfExists Whether database should be dropped only if it exists (default = true)
      */
    def dropDatabase(databaseName: String, checkIfExists: Boolean = true) =
        execute(s"DROP DATABASE${if (checkIfExists) " IF EXISTS " else " "}$databaseName")
    
    /**
      * Executes all statements read from the specified input file
      * @param inputPath Input file path
      * @return Success if all of the statements in the file were properly executed. Failure otherwise.
      */
    def executeStatementsFrom(inputPath: Path) = {
        // Reads the file and executes statements whenever one has been completely read
        IterateLines.fromPath(inputPath) { lines =>
            Try {
                var currentStatementBuilder = new StringBuilder()
                lines.map { _.trim }.filterNot { s => s.isEmpty || s.startsWith("--") }.foreach { line =>
                    splitToStatements(line) match
                    {
                        // Appends current statement until its end is found
                        case Right(partialStatement) =>
                            currentStatementBuilder ++= partialStatement
                            currentStatementBuilder += ' '
                        case Left((statementEnd, completeStatements, statementStart)) =>
                            // Completes and executes current statement
                            currentStatementBuilder ++= statementEnd
                            execute(currentStatementBuilder.result())
                            // Executes other complete statements on this line
                            completeStatements.foreach(execute)
                            // Starts building the next statement
                            currentStatementBuilder = new StringBuilder()
                            statementStart.foreach { s =>
                                currentStatementBuilder ++= s
                                currentStatementBuilder += ' '
                            }
                    }
                }
            }
        }.flatten
    }
    
    private def splitToStatements(statementString: String) = {
        if (statementString.contains(';'))
        {
            val parts = statementString.split(";").toVector.map { _.trim }.filterNot { _.isEmpty }
            if (parts.size > 1)
            {
                if (statementString.trim.endsWith(";"))
                    Left((parts.head, parts.tail, None))
                else
                    Left((parts.head, parts.drop(1).dropRight(1), Some(parts.last)))
            }
            else
                Left((parts.head, Vector(), None))
        }
        else
            Right(statementString)
    }
    
    private def printIfDebugging(message: => String) = if (Connection.settings.debugPrintsEnabled) println(message)
    
    private def setValues(statement: PreparedStatement, values: Seq[Value]) = 
    {
        values.indices.foreach { i =>
            Connection.sqlValueConverter(values(i)) match
            {
                case Some((objectValue, jdbcType)) => statement.setObject(i + 1, objectValue, jdbcType)
                case None => statement.setNull(i + 1, Types.NULL)
            }
        }
    }
    
    private def stringFromResult(result: ResultSet, index: Int) = Option(result.getString(index))
    
    private def rowsFromResult(resultSet: ResultSet, tables: Iterable[Table]) = 
    {
        val meta = resultSet.getMetaData
        
        // Sorts the column indices for targeted tables
        val indicesForTables = Vector.range(1, meta.getColumnCount + 1).groupBy { 
                index => tables.find { _.name == meta.getTableName(index) } }
        // Maps each index to a column in a targeted table, flattening the map as well
        // Resulting map: Table -> (Column, sqlType, index)
        val columnIndices = indicesForTables.flatMap { case (tableOption, indices) => 
                tableOption.map { table => (table, indices.flatMap { 
                index => table.findColumnWithColumnName( meta.getColumnName(index) ).map {
                (_, meta.getColumnType(index), index) } }) }
        }
        // [(name, sqlType, index)]
        val nonColumnIndices = indicesForTables.getOrElse(None, Vector())
            .map { index => (meta.getColumnName(index), meta.getColumnType(index), index) }
        val hasContentOutsideTables = nonColumnIndices.nonEmpty
        
        // Parses the rows from the resultSet
        val rowBuffer = Vector.newBuilder[Row]
        while (resultSet.next())
        {
            // Reads the object data from each row, parses them into constants and creates a model 
            // The models are mapped to each table separately
            // Also includes data outside the tables if present
            val otherData =
            {
                if (hasContentOutsideTables)
                    Model.withConstants(nonColumnIndices.map { case (name, sqlType, index) =>
                        Constant(name, Connection.sqlValueGenerator(resultSet.getObject(index), sqlType)) })
                else
                    Model.empty
            }
            // NB: view.force is added in order to create a concrete map
            rowBuffer += Row(columnIndices.view.mapValues { data =>
                Model.withConstants(data.map { case (column, sqlType, index) => Constant(column.name,
                Connection.sqlValueGenerator(resultSet.getObject(index), sqlType)) })
            }.toMap, otherData)
        }
        
        rowBuffer.result()
    }
    
    private def generatedKeysFromResult(statement: Statement, tables: IterableOnce[Table]) =
    {
        // Retrieves keys as ints if all of the tables (that use indexing) use int as key type
        val useInt = tables.iterator.forall { _.primaryColumn.forall { _.dataType == IntType } }
        val results = statement.getGeneratedKeys
        val keyBuffer = Vector.newBuilder[Value]
        
        while (results.next())
        {
            val key: Value = if (useInt) results.getInt(1) else results.getLong(1)
            if (key.isDefined)
                keyBuffer += key
        }
        
        keyBuffer.result()
    }
}
