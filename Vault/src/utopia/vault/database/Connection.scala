package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.error.EnvironmentNotSetupException
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueConverterManager
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.string.IterateLines
import utopia.flow.util.TryExtensions._
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Pointer
import utopia.vault.database.Connection.settings
import utopia.vault.model.immutable.{Column, Result, Row, Table}
import utopia.vault.sql.SqlSegment

import java.nio.file.Path
import java.sql.{DriverManager, PreparedStatement, ResultSet, SQLException, Statement, Types}
import scala.collection.immutable.HashSet
import scala.util.{Failure, Success, Try}

object Connection
{
    // ATTRIBUTES    ------------------------
    
    /**
     * The converter that converts values to sql compatible format
     */
    val sqlValueConverter = new ValueConverterManager(Single(BasicSqlValueConverter))
    /**
     * The generator that converts sql data (object + type) into a value
     */
    val sqlValueGenerator = new SqlValueGeneratorManager(Single(BasicSqlValueGenerator))
    
    /**
     * The settings used for establishing new connections
     */
    var settings = ConnectionSettings()
    
    // If an external driver is used in database operations, it is stored here after instantiation
    private var driver: Option[Any] = None
    
    
    // COMPUTED -----------------------------
    
    /**
     * @return Whether debugging printing is currently enabled
     */
    def debugPrintsEnabled = settings.debugPrintsEnabled
    def debugPrintsEnabled_=(enable: Boolean) = modifySettings(_.copy(debugPrintsEnabled = enable))
    
    
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
    
    // Name of the user-targeted, but not necessarily currently used, database
    private var targetDbName = initialDBName.orElse { settings.defaultDBName }
    // Name of the currently used database (from the connection's perspective)
    private var usedDbName: Option[String] = None
    
    private val openConnectionPointer = Lazy.deprecating {
        Try {
            val settings = Connection.settings
            // Sets up the driver
            settings.driver.foreach { driver =>
                if (Connection.driver.isEmpty)
                    Connection.driver = Some(Class.forName(driver).newInstance())
            }
            val targetNoForwardSlash = {
                val base = settings.connectionTarget
                if (base.endsWith("/")) base.dropRight(1) else base
            }
            val fullTarget = targetDbName match {
                case Some(dbName) => s"$targetNoForwardSlash/$dbName"
                case None => targetNoForwardSlash
            }
            // Instantiates the connection
            val connection = DriverManager.getConnection(s"$fullTarget${ settings.charsetString }",
                settings.user, settings.password)
            
            // Sets up used database, if possible
            targetDbName match {
                case Some(dbName) => _use(connection, dbName)
                case None => usedDbName = None
            }
        
            connection
        }.getOrMap { e =>
            throw NoConnectionException(s"Failed to open a database connection with settings ${
                Connection.settings} and database '$targetDbName'", e)
        }
    } { !_.isClosed }
    
    /**
     * The name of the database the connection is used for. This is either defined by<br>
     * a) specifying the database name upon connection creation<br>
     * b) specified after the connection has been instantiated by assigning a new value<br>
     * c) the default option specified in the connection settings
     */
    def dbName = targetDbName
    def dbName_=(databaseName: String) = {
        if (!targetDbName.contains(databaseName)) {
            targetDbName = Some(databaseName)
            openConnectionPointer.current.foreach { connection => _use(connection, databaseName) }
        }
    }
    
    private def connection = openConnectionPointer.value
    
    
    // COMPUTED PROPERTIES    -------
    
    /**
     * Whether the connection to the database has already been established
     */
    def isOpen = openConnectionPointer.isInitialized
    
    // Returns a connection that targets a specific database
    private def targetedConnection = {
        val c = connection
        usedDbName match {
            case Some(_) => c
            case None =>
                targetDbName match {
                    case Some(dbName) =>
                        _use(c, dbName)
                        c
                    case None => throw NoConnectionException("Targeted database hasn't been specified")
                }
        }
    }
    
    
    // IMPLEMENTED  -----------------
    
    // Exceptions during closing are ignored
    override def close() = Try { openConnectionPointer.popCurrent().foreach { _.close() } }
    
    
    // OPERATORS    -----------------
    
    /**
     * Executes an sql statement and returns the results. The provided statement instance provides 
     * the exact parameters for the operation
     */
    def apply(statement: SqlSegment): Result = 
    {
        printIfDebugging(s"Executing statement: ${ statement.description }")
        val selectedTables: Set[Table] = if (statement.isSelect) statement.targetTables else HashSet()
        
        // Changes database if necessary
        statement.databaseName.foreach { dbName = _ }
        // Executes the query
        val c = statement.databaseName match {
            case Some(dbName) => connectionTargeting(dbName)
            case None => targetedConnection
        }
        val result = _apply(c, statement.sql, statement.values, selectedTables, statement.generatesKeys)
        
        // Fires database triggers / events, if necessary
        usedDbName.foreach { databaseName =>
            statement.events.foreach { _(result).foreach { event => Triggers.deliver(databaseName, event) } }
        }
        
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
     * @param requireTargetedDb Whether a database selection should be required at this point (default = false)
     * @return The results of the query, containing the read rows and keys. If 'selectedTables' 
     * parameter was empty, no rows are included. If 'returnGeneratedKeys' parameter was false, 
     * no keys are included. On update statements, includes number of updated rows.
      * @throws DBException If query failed for some reason
     */
    @throws(classOf[DBException])
    def apply(sql: String, values: Seq[Value], selectedTables: Set[Table] = HashSet(), 
            returnGeneratedKeys: Boolean = false, requireTargetedDb: Boolean = false) =
        _apply(possiblyTargetedConnection(requireTargetedDb), sql, values, selectedTables, returnGeneratedKeys)
    
    
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
    def tryExec(statement: SqlSegment) = Try(apply(statement))
    
    /**
     * Executes a simple sql string. Does not retrieve any values from the query.
     * @param requireTargetedDb Whether a database target should be required at this point (default = false)
     */
    @throws(classOf[EnvironmentNotSetupException])
    @throws(classOf[NoConnectionException])
    @throws(classOf[SQLException])
    def execute(sql: String, requireTargetedDb: Boolean = false): Unit = {
        // Empty statements are not executed
        if (sql.nonEmpty)
            _executeWith(if (requireTargetedDb) targetedConnection else connection, sql)
    }
    
    /**
     * Executes a query that allows use of prepared values. Reads and returns the resulting 
     * column data. Most of the time, using different versions of apply is better than using this method, 
     * but this one can be used without table data.
     * @param sql The sql string. Slots for values are indicated with question marks (?)
     * @param values the values inserted to the query. There should be a matching amount of values 
     * and slots in the sql string.
     * @param requireTargetedDb Whether targeted database should be specified at this point
     * @return A map for each row. The map contains column name + column value pairs. Only non-null 
     * values are included.
     */
    @throws(classOf[EnvironmentNotSetupException])
    @throws(classOf[NoConnectionException])
    @throws(classOf[SQLException])
    def executeQuery(sql: String, values: Seq[Value] = Empty, requireTargetedDb: Boolean = false) =
        _executeQuery(possiblyTargetedConnection(requireTargetedDb), sql, values)
    
    /**
      * Checks whether a database with specified name exists
      * @param databaseName Database name
      * @return Whether such a database exists
      */
    def existsDatabaseWithName(databaseName: String) = executeQuery(
        "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ? LIMIT 1",
        Single(databaseName)).nonEmpty
    
    /**
      * Checks whether there exists a database table combination
      * @param databaseName Database name
      * @param tableName Table name
      * @return Whether such a table exists in a database with that name
      */
    def existsTable(databaseName: String, tableName: String) = executeQuery(
        "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? LIMIT 1",
        Pair(databaseName, tableName)).nonEmpty
    
    /**
      * Creates a new database
      * @param databaseName Name of the new database
     *  @param defaultCharset Default character set to use in this database (optional, as a string)
     *  @param defaultCollate Default collate to assign to this database (optional, as a string)
      * @param checkIfExists Whether database should only be created if one doesn't exist yet (default = true)
      * @param useNewDb Whether connection should switch to target the new database afterwards (default = true)
      */
    def createDatabase(databaseName: String, defaultCharset: Option[String] = None,
                       defaultCollate: Option[String] = None, checkIfExists: Boolean = true,
                       useNewDb: Boolean = true) =
    {
        val ifExistsStr = if (checkIfExists) " IF EXISTS" else ""
        val charsetStr = defaultCharset match {
            case Some(charset) => s" DEFAULT CHARSET $charset"
            case None => ""
        }
        val collateStr = defaultCollate match {
            case Some(collate) => s" DEFAULT COLLATE $collate"
            case None => ""
        }
        val c = connection
        _executeWith(c, s"CREATE DATABASE$ifExistsStr $databaseName$charsetStr$collateStr")
        if (useNewDb) {
            _use(c, databaseName)
            targetDbName = Some(databaseName)
        }
    }
    
    /**
      * Drops / removes a database
      * @param databaseName Database name
      * @param checkIfExists Whether database should be dropped only if it exists (default = true)
      */
    def dropDatabase(databaseName: String, checkIfExists: Boolean = true) =
    {
        execute(s"DROP DATABASE${if (checkIfExists) " IF EXISTS " else " "}$databaseName")
        usedDbName = usedDbName.filterNot { _ == databaseName }
    }
    
    /**
      * Executes all statements read from the specified input file
      * @param inputPath Input file path
     *  @param requireTargetedDb Whether a specific database target should be required at this point
      * @return Success if all of the statements in the file were properly executed. Failure otherwise.
      */
    def executeStatementsFrom(inputPath: Path, requireTargetedDb: Boolean = false) = {
        // Reads the file and executes statements whenever one has been completely read
        IterateLines.fromPath(inputPath) { lines =>
            Try {
                val c = possiblyTargetedConnection(requireTargetedDb)
                var currentStatementBuilder = new StringBuilder()
                lines.map { _.trim }.filterNot { s => s.isEmpty || s.startsWith("--") }.foreach { line =>
                    splitToStatements(line) match {
                        // Appends current statement until its end is found
                        case Right(partialStatement) =>
                            currentStatementBuilder ++= partialStatement
                            currentStatementBuilder += ' '
                        case Left((statementEnd, completeStatements, statementStart)) =>
                            // Completes and executes current statement
                            currentStatementBuilder ++= statementEnd
                            _executeWith(c, currentStatementBuilder.result())
                            // Executes other complete statements on this line
                            completeStatements.foreach { _executeWith(c, _) }
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
    
    @throws(classOf[DBException])
    private def _apply(connection: java.sql.Connection, sql: String, values: Seq[Value],
                       selectedTables: Set[Table] = HashSet(), returnGeneratedKeys: Boolean = false) =
    {
        // Empty statements are not executed
        if (sql.isEmpty)
            Result.empty
        else {
            Try {
                // Creates the statement
                connection.prepareStatement(sql,
                    if (returnGeneratedKeys) Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS)
                    .consume { statement =>
                        // Inserts provided values
                        setValues(statement, values)
                        // Executes the statement and retrieves the result (if available)
                        val foundResult = statement.execute()
                        // Case: Expecting generated keys
                        if (returnGeneratedKeys)
                            Result(Empty, generatedKeysFromResult(statement, selectedTables))
                        else {
                            // Collects the result rows or update count from the first result
                            var rows: Seq[Row] = {
                                if (foundResult)
                                    statement.getResultSet.consume { rowsFromResult(_, selectedTables) }
                                else
                                    Empty
                            }
                            var updateCount = if (foundResult) 0 else statement.getUpdateCount
                            // Handles possible additional results
                            var expectsMore = foundResult || updateCount >= 0
                            while (expectsMore) {
                                // Case: Additional result with more rows
                                if (statement.getMoreResults)
                                    rows = rows ++ statement.getResultSet.consume { rowsFromResult(_, selectedTables) }
                                else {
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
            } match {
                case Success(result) => result
                case Failure(error) => throw new DBException(
                    s"DB query failed.\nSql: $sql\nValues:[${values.mkString(", ")}]", error)
            }
        }
    }
    
    @throws(classOf[EnvironmentNotSetupException])
    @throws(classOf[NoConnectionException])
    @throws(classOf[SQLException])
    private def _executeQuery(connection: java.sql.Connection, sql: String, values: Seq[Value] = Empty) =
    {
        // Empty statements are not executed
        if (sql.isEmpty)
            Empty
        else {
            // Creates the statement
            connection.prepareStatement(sql).consume { statement =>
                // Inserts provided values
                setValues(statement, values)
                
                // Executes the statement and retrieves the result
                statement.executeQuery().consume { results =>
                    val meta = results.getMetaData
                    
                    val columnIndices = (1 to meta.getColumnCount)
                        .map { index => (meta.getColumnName(index), index) }
                    
                    // Parses data out of the result
                    val buffer = Vector.newBuilder[Map[String, String]]
                    while (results.next()) {
                        buffer += columnIndices.flatMap { case (name, index) =>
                            stringFromResult(results, index).map { (name, _) } }.toMap
                    }
                    
                    buffer.result()
                }
            }
        }
    }
    
    // Makes sure the connection targets the specified database
    private def connectionTargeting(databaseName: String) = {
        val c = connection
        if (!usedDbName.contains(databaseName))
            _use(c, databaseName)
        c
    }
    
    private def possiblyTargetedConnection(shouldTarget: Boolean) = {
        if (shouldTarget)
            targetedConnection
        else {
            val c = connection
            targetDbName.filter { !usedDbName.contains(_) }.foreach { _use(c, _) }
            c
        }
    }
    
    private def _use(connection: java.sql.Connection, dbName: String) = {
        _executeWith(connection, s"USE $dbName")
        usedDbName = Some(dbName)
    }
    
    private def _executeWith(connection: java.sql.Connection, sql: String) =
        connection.createStatement().consume { _.executeUpdate(sql) }
    
    // Returns either:
    //      Left: 1) Statement or the end of a statement, 2) Complete statements and 3) Partial ending statement, if applicable
    //      Right: Statement (possibly partial)
    private def splitToStatements(statementString: String) = {
        if (statementString.contains(';')) {
            val parts = statementString.split(";").view.map { _.trim }.filterNot { _.isEmpty }.toOptimizedSeq
            if (parts.hasSize > 1) {
                if (statementString.trim.endsWith(";"))
                    Left((parts.head, parts.tail, None))
                else
                    Left((parts.head, parts.slice(1, parts.size - 1), Some(parts.last)))
            }
            else
                Left((parts.head, Empty, None))
        }
        else
            Right(statementString)
    }
    
    private def printIfDebugging(message: => String) = if (Connection.settings.debugPrintsEnabled) println(message)
    
    private def setValues(statement: PreparedStatement, values: Seq[Value]) = {
        values.indices.foreach { i =>
            Connection.sqlValueConverter(values(i)) match {
                case Some((objectValue, jdbcType)) => statement.setObject(i + 1, objectValue, jdbcType)
                case None => statement.setNull(i + 1, Types.NULL)
            }
        }
    }
    
    private def stringFromResult(result: ResultSet, index: Int) = Option(result.getString(index))
    
    private def rowsFromResult(resultSet: ResultSet, tables: Iterable[Table]) = {
        val meta = resultSet.getMetaData
        
        // Groups the column indices by targeted tables
        val indicesForTables = (1 to meta.getColumnCount)
            .groupBy { index => tables.find { _.name == meta.getTableName(index) } }
        // Maps each index to a column in a targeted table, flattening the map as well
        // Resulting map: Table -> (Option[(Column, sqlType, index)] -> [(Column, sqlType, index)])
        //      Where the first column is primary column (None if no primary column exists)
        val columnIndices = indicesForTables.flatMap { case (tableOption, indices) => 
            tableOption.map { table =>
                val columns = indices.flatMap { index =>
                    table.findColumnWithColumnName(meta.getColumnName(index))
                        .map { (_, meta.getColumnType(index), index) }
                }
                val (primaryColumn, otherColumns) = columns.findAndPop { _._1.isPrimary }
                table -> (primaryColumn, otherColumns)
            }
        }
        // [(name, sqlType, index)]
        val nonColumnIndices = indicesForTables.getOrElse(None, Empty)
            .map { index => (meta.getColumnName(index), meta.getColumnType(index), index) }
        val hasContentOutsideTables = nonColumnIndices.nonEmpty
        val isMultiTableQuery = tables.hasSize > 1
        
        // Contains the last read primary key -> model pair for each targeted table
        // Used for skipping model-parsing in case the primary key stays the same
        // (which is often the case when dealing with one-to-many joins)
        lazy val lastModelsMap = columnIndices.map { case (table, _) =>
            table -> Pointer[(Value, Model)](Value.empty -> Model.empty)
        }
        
        def valueFrom(index: Int, dataType: Int) = Connection.sqlValueGenerator(resultSet.getObject(index), dataType)
        
        // Here, colData is same as an entry in columnIndices
        def constantFrom(colData: (Column, Int, Int)) = Constant(colData._1.name, valueFrom(colData._3, colData._2))
        
        // Parses the rows from the resultSet
        OptionsIterator
            .continually {
                if (resultSet.next()) {
                    // Reads the object data from each row, parses them into constants and creates a model
                    // The models are mapped to each table separately
                    // Also includes data outside the tables if present
                    val tableModels = columnIndices.map { case (table, (primaryColumnData, otherColumnData)) =>
                        primaryColumnData match {
                            // Case: This table has a primary column
                            case Some((primaryColumn, primaryColType, primaryColIndex)) =>
                                val model = {
                                    // Case: Multi-table query => Checks whether this row is similar to the previous
                                    //                            (in terms of this table's columns)
                                    if (isMultiTableQuery) {
                                        val primaryKey = valueFrom(primaryColIndex, primaryColType)
                                        lastModelsMap(table).mutate { previous =>
                                            // Case: The primary key of previous row matches the one on this row
                                            //       => Skips model-parsing and uses the previously acquired model
                                            if (primaryKey == previous._1)
                                                previous._2 -> previous
                                            // Case: This row's primary key is different => Parses this row as a new model
                                            else {
                                                val model = Model.withConstants(Constant(primaryColumn.name, primaryKey) +:
                                                    otherColumnData.map(constantFrom))
                                                model -> (primaryKey -> model)
                                            }
                                        }
                                    }
                                    // Case: Single table query => Won't check for repeating rows
                                    else
                                        Model.withConstants(
                                            ((primaryColumn, primaryColType, primaryColIndex) +: otherColumnData)
                                                .map(constantFrom))
                                }
                                table -> model
                                
                            // Case: Primary key not used in this table => Parses the row into a model normally
                            case None => table -> Model.withConstants(otherColumnData.map(constantFrom))
                        }
                    }
                    val otherData = {
                        if (hasContentOutsideTables)
                            Model.withConstants(nonColumnIndices.map { case (name, sqlType, index) =>
                                Constant(name, Connection.sqlValueGenerator(resultSet.getObject(index), sqlType)) })
                        else
                            Model.empty
                    }
                    Some(Row(tableModels, otherData))
                }
                else
                    None
            }
            .toOptimizedSeq
    }
    
    private def generatedKeysFromResult(statement: Statement, tables: Iterable[Table]) = {
        // Retrieves keys as integers, if all of the tables (that use indexing) use int as key type
        val useInt = tables.forall { _.primaryColumn.forall { _.dataType == IntType } }
        val results = statement.getGeneratedKeys
        
        OptionsIterator
            .continually {
                if (results.next())
                    Some[Value](if (useInt) results.getInt(1) else results.getLong(1))
                else
                    None
            }
            .toOptimizedSeq
    }
}
