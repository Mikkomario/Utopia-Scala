package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.error.EnvironmentNotSetupException
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueConverterManager
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.operator.ScopeUsable
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.string.Lines
import utopia.flow.util.StringExtensions._
import utopia.flow.util.result.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.mutable.eventful.{AssignableOnce, SettableFlag}
import utopia.vault.database.Connection.{GeneratedKeysIterator, StatementRowsIterator, settings}
import utopia.vault.error.HandleError
import utopia.vault.model.error.{DBException, NoConnectionException}
import utopia.vault.model.immutable.{Result, Row, Table}
import utopia.vault.model.mutable.ResultStream
import utopia.vault.sql.SqlSegment

import java.nio.file.Path
import java.sql._
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
	def doTransaction[T](f: Connection => T) = {
		import utopia.vault.context.VaultContext.log
		new Connection().consume(f)
	}
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
	
	
	// NESTED   ----------------------------------
	
	private class StatementRowsIterator(statement: PreparedStatement, failureP: AssignableOnce[Throwable],
	                                    closedFlag: View[Boolean], tableIndex: Map[String, Table])
	                                   (implicit log: Logger)
		extends Iterator[Row]
	{
		// ATTRIBUTES   --------------------------
		
		/**
		  * Whether row-processing should avoid parsing duplicate rows.
		  * Duplicate rows are only possible when joining multiple tables (in one-to-many fashion).
		  * The duplicate checking requires access to primary column values.
		  */
		private lazy val shouldCheckForDuplicateRows =
			tableIndex.hasSize > 1 && tableIndex.valuesIterator.exists { _.hasPrimaryColumn }
		
		/**
		  * A pointer that contains the currently open result-set instance.
		  * Contains None once no more results are available.
		  */
		// Attempts to acquire the initial result-set
		private lazy val openResultsP = Pointer.eventful(Try { Option(statement.getResultSet) }.getOrMap { error =>
			// Case: Failed to acquire the initial result-set => Records the failure
			failureP.trySet(error)
			None
		})
		/**
		  * An iterator that yields additional result-sets.
		  * Should only be called once [[openResultsP]] becomes empty.
		  */
		private lazy val moreResultsIterator = OptionsIterator
			.continually { if (statement.getMoreResults) Option(statement.getResultSet) else None }
		/**
		  * A lazily performed check to current result-set's next() function.
		  * Attempts to acquire a new result-set, if necessary.
		  *
		  * Contains true if more rows are available. Contains false if no more rows may be acquired.
		  * Should be reset when reading / consuming the row in question.
		  */
		private val lazyNextResult = ResettableLazy {
			// Makes sure there's a result-set available
			openResultsP.value.exists { results =>
				// Attempts to move to the next row
				Try { results.next() } match {
					case Success(hasNext) =>
						// Case: Next row is available
						if (hasNext)
							true
						// Case: Next row is not available => Attempts to acquire a new result-set
						else
							Try {
								// Acquires new result-sets until a non-empty set is found
								moreResultsIterator.exists { nextResults =>
									// Caches the results as they arrive
									openResultsP.value = Some(nextResults)
									
									// Checks whether the first row is available
									val nonEmpty = nextResults.next()
									
									// If the result-set was empty, clears the cache immediately
									if (!nonEmpty)
										openResultsP.clear()
									
									nonEmpty
								}
							}.getOrMap { error =>
								// Case: Failed to acquire the next result-set => No more rows + error state
								failureP.trySet(error)
								false
							}
						
					// Case: Moving failed => No more rows available (+ error state activates)
					case Failure(error) =>
						failureP.trySet(error)
						false
				}
			}
		}
		/**
		  * A pointer that contains a function for parsing the currently active result-set-
		  * Contains None while no rows may be acquired
		  * (i.e. when there are no results to parse or when failure state activates)
		  */
		private lazy val readRowP = openResultsP.map[Option[() => Row]] { results =>
			results.flatMap[() => Row] { results =>
				Try {
					// Acquires the result-set metadata and uses that to form the parsing functions
					val meta = results.getMetaData
					val columnIndices = 1 to meta.getColumnCount
					
					// Checks which read column belongs to which table
					// Some columns may be outside the specified tables
					val (otherIndices, tableIndices) = columnIndices.groupBy(meta.getTableName)
						.divideWith { case (tableName, indices) =>
							tableIndex.get(tableName) match {
								// Case: Targeting one of the read tables
								case Some(table) => Right(table -> indices)
								// Case: Targeting some other data
								case None => Left(tableName -> indices)
							}
						}
					
					// Maps column indices to database property names.
					// Acquires these names from the matching columns.
					val columnNames = columnIndices.view.map { i => i -> meta.getColumnName(i) }.toMap
					val propNames = tableIndices.view
						.flatMap { case (table, indices) =>
							indices.map { i =>
								val name = columnNames.get(i) match {
									case Some(colName) =>
										table.findColumnWithName(colName) match {
											case Some(column) => column.name
											// Case: No matching column found => Defaults to the column name
											case None => colName
										}
										
									// Case: No column name available => Applies a placeholder name
									case None => "other"
								}
								i -> name
							}
						}
						// If no column is available, uses the read column name.
						.toMap.withDefault { i => columnNames.getOrElse(i, "other") }
					
					// Determines a value conversion & acquisition function for each read column
					val sqlConversions = Cache { sqlType: Int => sqlValueGenerator.conversionFrom(sqlType) }
					val getColumnValue = columnIndices.view
						.flatMap { i =>
							// Converts the SQL-specific type to a value-conversion function, if possible
							val sqlType = meta.getColumnType(i)
							val getValue = sqlConversions(sqlType)
								// Converts the conversion-function into a value read -function
								.map { valueFrom => () => valueFrom(results.getObject(i)) }
							
							// If conversion is not possible, logs a warning
							if (getValue.isEmpty)
								log(s"No value conversion is possible from SQL type $sqlType for column ${
									columnNames(i) }")
									
							getValue.map { i -> _ }
						}
						// Yields empty values for columns which cannot be read or converted
						.toMap.withDefaultValue(() => Value.empty)
					
					// Functions for converting column indices into a model
					def customIndicesToModel(indices: Iterable[Int])(getValue: Int => Value) = {
						if (indices.isEmpty)
							Model.empty
						else
							Model.withConstants(indices.map { i => Constant(propNames(i), getValue(i)) })
					}
					def indicesToModel(indices: Iterable[Int]) = customIndicesToModel(indices) { getColumnValue(_)() }
					
					// Function for reading data from colums which didn't match any of the predefined tables
					val readOtherModels = {
						if (otherIndices.isEmpty)
							() => Empty
						else
							() => otherIndices.map { case (tableName, indices) => tableName -> indicesToModel(indices) }
					}
					
					// Case: There may be duplicate model entries => Prepares to perform duplicate-checking
					if (shouldCheckForDuplicateRows &&
						(tableIndices.hasSize > 1 || (otherIndices.nonEmpty && tableIndices.hasSize(1))))
					{
						// Checks which read columns match primary table columns
						val primaryColumnIndices = tableIndices.view
							.flatMap { case (table, indices) =>
								table.primaryColumn.flatMap { col =>
									indices.find { i => columnNames.get(i).contains(col.columnName) }
										.map { table.name -> _ }
								}
							}
							.toMap
						
						// Case: Primary columns are not being read
						//       => Duplicate-checking is impossible
						//       => Performs the default parsing instead
						if (primaryColumnIndices.isEmpty)
							() => {
								val tableModelsView = tableIndices.view
									.map { case (table, indices) => table.name -> indicesToModel(indices) }
								Row((tableModelsView ++ readOtherModels()).toMap.withDefaultValue(Model.empty))
							}
						// Case: Duplicate-checking is possible => Prepares a function which handles it
						else {
							// Stores the last read model (as well as the matching primary key value) for each table
							// If the primary key value of the following row matches, uses the previously parsed model,
							// skipping value-processing
							var lastModels = Map[String, Pointer[(Value, Model)]]()
							() => {
								val tableModelsView = tableIndices.view
									.map { case (table, indices) =>
										val model = primaryColumnIndices.get(table.name) match {
											case Some(primaryIndex) =>
												// Reads the primary key value
												// and checks if that matches the previously read value, if applicable
												val primaryValue = getColumnValue(primaryIndex)()
												lastModels.get(table.name) match {
													// Case: Rows have been read previously
													//       => Checks whether this one is a duplicate
													case Some(lastModelP) =>
														lastModelP.mutate { case (lastPrimaryValue, lastModel) =>
															// Case: Duplicate => Yields the previously parsed model
															if (primaryValue == lastPrimaryValue)
																lastModel -> (lastPrimaryValue, lastModel)
															// Case: Not a duplicate
															//       => Parses the new model and remembers it
															else {
																// Won't process the primary key value again
																val newModel = customIndicesToModel(indices) { i =>
																	if (i == primaryIndex)
																		primaryValue
																	else
																		getColumnValue(i)()
																}
																newModel -> (primaryValue, newModel)
															}
														}
													// Case: No rows of this table have been read yet
													//       => Won't perform duplicate-checking
													case None =>
														// Won't process the primary key value again
														val model = customIndicesToModel(indices) { i =>
															if (i == primaryIndex)
																primaryValue
															else
																getColumnValue(i)()
														}
														// Remembers the parsed model for future rows
														lastModels += table.name -> Pointer(primaryValue -> model)
														model
												}
											
											// Case: No primary column is read for this table => Uses default parsing
											case None => indicesToModel(indices)
										}
										table.name -> model
									}
								
								Row((tableModelsView ++ readOtherModels()).toMap.withDefaultValue(Model.empty))
							}
						}
					}
					// Case: Duplicate-checking is not necessary => Applies the default parsing logic
					else
						() => {
							val tableModelsView = tableIndices.view
								.map { case (table, indices) => table.name -> indicesToModel(indices) }
							Row((tableModelsView ++ readOtherModels()).toMap.withDefaultValue(Model.empty))
						}
				} match {
					case Success(getRow) => Some(getRow)
					// Case: Something failed during the function-preparation
					//       => Row-parsing is not possible, and error state activates
					case Failure(error) =>
						failureP.trySet(error)
						None
				}
			}
		}
		
		
		// IMPLEMENTED  --------------------------
		
		// If failure state has activated or the results closed, won't check for additional rows
		// Parsing must also be possible
		override def hasNext: Boolean =
			!closedFlag.value && failureP.isNotSet && lazyNextResult.value && readRowP.value.isDefined
		
		override def next(): Row = {
			// Makes sure a new row has been prepared
			// Case: A row is available => Proceeds to parse it, if possible
			if (lazyNextResult.pop())
				readRowP.value match {
					case Some(readRow) => readRow()
					// Case: Row-parsing is not possible => Throws
					case None =>
						throw failureP.value.getOrElse { new IllegalStateException("No more rows may be read") }
				}
			else
				throw failureP.value
					.getOrElse { new IllegalStateException("next() called when there are no more results available") }
		}
		
		
		// OTHER    ---------------------------
		
		/**
		  * Consumes all the remaining result-sets without parsing any rows
		  * @return Success or a failure
		  */
		def flush() = {
			// Case: Results are available => Consumes any remaining results
			if (openResultsP.pop().isDefined) {
				val result = Iterator.continually { Try { statement.getMoreResults() } }
					.takeTo { _.toOption.forall { !_ } }.last
				// If failed, activates failure mode
				result.failure.foreach { failureP.trySet(_) }
				result.map { _ => () }
			}
			// Case: No more results available => Determines whether 'cause of a failure or a normal state
			else
				failureP.value match {
					case Some(error) => Failure(error)
					case None => Success(())
				}
		}
	}
	
	private class GeneratedKeysIterator(statement: Statement, failureP: AssignableOnce[Throwable],
	                                    closedFlag: View[Boolean])
		extends Iterator[Value]
	{
		// ATTRIBUTES   ------------------------
		
		/**
		  * A result-set which contains the generated keys
		  */
		// Acquires the result-set lazily
		private lazy val results = Try { statement.getGeneratedKeys } match {
			case Success(results) => Some(results)
			// Case: Failed to acquire a result-set => Activates failure mode
			case Failure(error) =>
				failureP.trySet(error)
				None
		}
		/**
		  * A lazy container for checking the next() of the used result-set.
		  * Should be reset when parsing a row.
		  */
		private val lazyNextResult = ResettableLazy {
			// The result-set must be available, obviously
			results.exists { results =>
				// Tests whether the next row is available
				// If fails, activates failure mode
				Try { results.next() }.getOrMap { error =>
					failureP.trySet(error)
					false
				}
			}
		}
		/**
		  * A (lazily initialized) function for reading and parsing the generated key of the currently selected row.
		  * None if parsing is not possible.
		  */
		private lazy val readValue = results.flatMap { results =>
			Try {
				// Uses result-set metadata in order to determine how read values should be interpreted
				val meta = results.getMetaData
				val sqlType = meta.getColumnType(1)
				val getValue = sqlValueGenerator.conversionFrom(sqlType).map { valueFrom =>
					() => valueFrom(results.getObject(1))
				}
				// Case: Value-conversion is not supported for this data-type => Activates failure mode
				if (getValue.isEmpty)
					failureP.trySet {
						new IllegalArgumentException(s"Can't process generated keys of SQL type $sqlType")
					}
				
				getValue
				
			}.getOrMap { error =>
				failureP.trySet(error)
				None
			}
		}
		
		
		// IMPLEMENTED  ------------------------
		
		// Won't yield more items after closed or failed
		// Parsing must also be possible
		override def hasNext: Boolean =
			!closedFlag.value && failureP.isNotSet && lazyNextResult.value && readValue.isDefined
		
		override def next(): Value = {
			// Makes sure the next value is available
			// Case: Value available => Reads it, if possible
			if (lazyNextResult.pop()) {
				readValue match {
					case Some(getValue) => getValue()
					// Case: Generated keys can't be read => Throws
					case None =>
						throw failureP.value
							.getOrElse { new IllegalStateException("Can't parse the acquired generated keys") }
				}
			}
			// Case: No more values available => Throws
			else
				throw failureP.value
					.getOrElse { new IllegalStateException(s"next() called when there are no more keys available") }
		}
	}
}

/**
  * Instances of this class handle database connections and allow low level database interaction
  * through SQL statements
  * @author Mikko Hilpinen
  * @since 16.4.2017
  */
class Connection(initialDBName: Option[String] = None)(implicit log: Logger)
	extends AutoCloseable with ScopeUsable[Connection]
{
	// ATTRIBUTES    -----------------
	
	// Name of the user-targeted, but not necessarily currently used, database
	private var targetDbName = initialDBName.orElse { settings.defaultDBName }
	// Name of the currently used database (from the connection's perspective)
	private var usedDbName: Option[String] = None
	
	private val openConnectionPointer = Lazy.deprecating { openConnection() } { _.success.exists { !_.isClosed } }
	
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
			currentConnection.foreach { _use(_, databaseName) }
		}
	}
	
	private def connection = openConnectionPointer.value.getOrMap { error =>
		throw new NoConnectionException(s"Failed to open a database connection with settings ${
			Connection.settings} and database '$targetDbName'", error)
	}
	
	
	// COMPUTED PROPERTIES    -------
	
	/**
	  * Whether the connection to the database has already been established
	  */
	def isOpen = openConnectionPointer.isInitialized
	
	private def currentConnection = openConnectionPointer.current.flatMap { _.toOption }
	
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
					case None => throw new NoConnectionException("Targeted database hasn't been specified")
				}
		}
	}
	
	
	// IMPLEMENTED  -----------------
	
	override def self: Connection = this
	
	// Exceptions during closing are ignored
	override def close() = Try { currentConnection.foreach { _.close() } }
	
	
	// OTHER    ---------------------
	
	/**
	  * Makes sure the wrapped connection is currently usable.
	  * If no connection is open, attempts to open one. If the current connection is invalid, resets it.
	  * @param timeoutSeconds Maximum validation timeout, in seconds. 0 If not timeout shall be specified.
	  * @throws SQLException if 'timeoutSeconds' is less than 0.
	  * @return True if the currently wrapped connection is now valid and usable.
	  */
	@throws[SQLException]
	def validate(timeoutSeconds: => Int) = openConnectionPointer.value.success.exists { original =>
		// Case: Original connection is valid => Returns
		if (original.isValid(timeoutSeconds))
			true
		// Case: Original connection is invalid => Creates a new connection to replace it
		else {
			original.close()
			// Validates this new connection
			openConnectionPointer.newValue().success.exists { _.isValid(timeoutSeconds) }
		}
	}
	
	/**
	  * Performs an SQL query. Buffers the results.
	  * @param statement Statement to execute.
	  * @return Acquired result
	  */
	@throws[DBException]("If a database exception was encountered")
	def apply(statement: SqlSegment): Result = {
		val result = stream(statement) { _.buffer }
		
		// Fires database triggers / events, if necessary
		usedDbName.foreach { databaseName =>
			statement.events.foreach { _(result).foreach { event => Triggers.deliver(databaseName, event) } }
		}
		
		printIfDebugging(s"Received result: $result")
		result
	}
	
	/**
	  * Performs an SQL query in a streaming fashion.
	  * Note: The current implementation will not trigger any database events.
	  * @param statement Statement to execute
	  * @param f A function for processing the results
	  * @tparam A Type of parsed results
	  * @return Parsed / processed results
	  */
	@throws[DBException]("If a database exception was encountered, or if 'f' threw an exception")
	def stream[A](statement: SqlSegment)(f: ResultStream => A) = {
		printIfDebugging(s"Executing statement: ${ statement.description }")
		val selectedTables: Iterable[Table] = if (statement.isSelect) statement.targetTables else Empty
		
		// Changes database if necessary
		statement.databaseName.foreach { dbName = _ }
		// Executes the query
		val c = statement.databaseName match {
			case Some(dbName) => connectionTargeting(dbName)
			case None => targetedConnection
		}
		
		// TODO: Add support for database triggers / events
		_stream(c, statement.sql, statement.values, selectedTables, statement.generatesKeys)(f)
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
	def apply(sql: String, values: Seq[Value], selectedTables: Set[Table] = Set(),
	          returnGeneratedKeys: Boolean = false, requireTargetedDb: Boolean = false) =
		_stream(possiblyTargetedConnection(requireTargetedDb), sql, values, selectedTables,
			returnGeneratedKeys) { _.buffer }
	
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
	@deprecated("Deprecated for removal. Please use .stream(Statement)(...) instead", "v1.22")
	def iterator(query: SqlSegment, rowsPerIteration: Int = Connection.settings.maximumAmountOfRowsCached) =
		new QueryIterator(query, rowsPerIteration)(this, log).filterNot { _.isEmpty }
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
	@deprecated("Deprecated for removal. Please use .stream(Statement)(...) instead", "v1.22")
	def rowIterator(query: SqlSegment, rowsPerIteration: Int = Connection.settings.maximumAmountOfRowsCached) =
		new QueryIterator(query, rowsPerIteration)(this, log).flatMap { _.rows }
	/**
	  * Performs an operation on each row targeted by specified statement
	  * @param statement a (select) statement. <b>Must not include limit or offset</b>
	  * @param operation Operation performed for each row
	  */
	@deprecated("Deprecated for removal. Please use .stream(Statement)(...) instead", "v1.22")
	def foreach(statement: SqlSegment)(operation: Row => Unit) =
		stream(statement) { _.rowsIterator.foreach(operation) }
	/**
	  * Maps read rows and reduces them into a single value. This should be used when handling queries which may
	  * yield very large results.
	  * @param statement A (select) statement. <b>Must not include limit or offset</b>
	  * @param map A mapping function for rows
	  * @param reduce A reduce function for mapped rows
	  * @tparam A Type of map result
	  * @return Reduction result. None if no data was read.
	  */
	@deprecated("Deprecated for removal. Please use .stream(Statement)(...) instead", "v1.22")
	def mapReduce[A](statement: SqlSegment)(map: Row => A)(reduce: (A, A) => A) =
		stream(statement) { _.rowsIterator.map(map).reduceOption(reduce) }
	/**
	  * Maps read rows and reduces them into a single value. This should be used when handling queries which may
	  * yield very large results.
	  * @param statement A (select) statement. <b>Must not include limit or offset</b>
	  * @param map A mapping function for rows (may return 0 or multiple values)
	  * @param reduce A reduce function for mapped rows
	  * @tparam A Type of map result
	  * @return Reduction result. None if no data was read.
	  */
	@deprecated("Deprecated for removal. Please use .stream(Statement)(...) instead", "v1.22")
	def flatMapReduce[A](statement: SqlSegment)(map: Row => IterableOnce[A])(reduce: (A, A) => A) =
		stream(statement) { _.rowsIterator.flatMap(map).reduceOption(reduce) }
	/**
	  * Folds read rows into a single value. This function may prove useful with very large queries.
	  * @param statement A (select) statement. <b>Must not include limit or offset</b>
	  * @param start Starting value
	  * @param f Folding function (takes previous result and adds one row to it)
	  * @tparam A Result type
	  * @return Fold result
	  */
	@deprecated("Deprecated for removal. Please use .stream(Statement)(...) instead", "v1.22")
	def fold[A](statement: SqlSegment)(start: A)(f: (A, Row) => A) =
		stream(statement) { _.rowsIterator.foldLeft(start)(f) }
		
	/**
	  * Tries to execute a statement. Wraps the results in a try
	  */
	@deprecated("Deprecated for removal", "v1.22")
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
	def dropDatabase(databaseName: String, checkIfExists: Boolean = true) = {
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
		Lines.iterate.path(inputPath) { linesIter =>
			val c = possiblyTargetedConnection(requireTargetedDb)
			var currentStatementBuilder = new StringBuilder()
			linesIter.map { _.trim }.filterNot { s => s.isEmpty || s.startsWith("--") }.foreach { line =>
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
	}
	
	private def openConnection() = Try {
		val settings = Connection.settings
		// Sets up the driver
		settings.driver.foreach { driver =>
			if (Connection.driver.isEmpty)
				Connection.driver = Some(Class.forName(driver).newInstance())
		}
		val targetNoForwardSlash = settings.connectionTarget.notEndingWith("/")
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
	}
	
	private def _stream[A](connection: java.sql.Connection, sql: String, values: Seq[Value],
	                       selectedTables: Iterable[Table] = Empty, returnGeneratedKeys: Boolean = false)
	                      (f: ResultStream => A) =
	{
		// Empty statements are not executed
		if (sql.isEmpty)
			f(ResultStream.empty)
		else {
			// Prepares the statement
			val result = Try
				.apply {
					connection.prepareStatement(sql,
						if (returnGeneratedKeys) Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS)
				}
				.flatMap { statement =>
					val closedFlag = SettableFlag()
					val result = statement.consume { statement =>
						Try {
							// Specifies the values and executes the statement
							setValues(statement, values)
							statement.execute()
							
						}.flatMap { containsResults =>
							// Forms the result stream
							val failureP = AssignableOnce[Throwable]()
							lazy val failureViewP = failureP.viewWhile(!closedFlag)
							val result = {
								// Case: Expecting generated keys
								if (returnGeneratedKeys) {
									val keysIter = new GeneratedKeysIterator(statement, failureP, closedFlag)
									new ResultStream(closedFlag, failureViewP, generatedKeysIterator = keysIter)
								}
								// Case: Results became available => Prepares a streamed result
								else if (containsResults) {
									// Prepares an iterator for row-reading
									val rowsIter = new StatementRowsIterator(statement, failureP, closedFlag,
										selectedTables.view.map { t => t.name -> t }.toMap)
									// Prepares a lazy interface for acquiring the update count
									val lazyUpdateCount = Lazy {
										failureP.value match {
											// Case: Already failed => Yields a failure
											case Some(error) => Failure(error)
											case None =>
												// Case: Already closed => Yields a failure
												if (closedFlag.isSet)
													Failure(new IllegalStateException(
														"This result stream has already closed"))
												// Case: Open => Flushes all remaining row content
												//               and attempts to acquire the update count
												else
													rowsIter.flush().flatMap { _ =>
														Try { statement.getUpdateCount max 0 }
													}
										}
									}
									new ResultStream(closedFlag, failureViewP, rowsIter,
										lazyUpdatedRowsCount = lazyUpdateCount)
								}
								// Case: No row content present => Prepares to acquire an update count, if appropriate
								else {
									// WET WET
									val lazyUpdateCount = Lazy {
										failureP.value match {
											case Some(error) => Failure(error)
											case None =>
												if (closedFlag.isSet)
													Failure(new IllegalStateException(
														"This result stream has already closed"))
												else
													Try { statement.getUpdateCount max 0 }
										}
									}
									new ResultStream(closedFlag, failureViewP, lazyUpdatedRowsCount = lazyUpdateCount)
								}
							}
							// Passes the result stream to the specified function, while the statement remains open
							// Catches errors
							Try { f(result) }.map { result =>
								// If a failure was encountered during the processing,
								// it is captured, and handled later
								result -> failureP.value
							}
						}
					}
					// Marks the statement (including the results) as closed
					closedFlag.set()
					
					result
				}
			
			// Handles errors encountered during statement preparation and execution,
			// As well as those thrown by the specified function
			val (lazyFinalResult, errorToHandle) = result match {
				// Case: 'f' ran successfully => Prepares an error based on failure-pointer's contents, if applicable
				case Success((result, error)) => Lazy.initialized(result) -> error
				// Case: 'f' threw an exception
				//       => Prepares the captured error.
				//          If it is not rethrown later, simulates a new return value with an empty result.
				case Failure(error) => Lazy { f(ResultStream.empty) } -> Some(error)
			}
			errorToHandle match {
				// Case: Encountered an error => Converts it to a detailed DBException
				//                               and lets HandleError deal with it
				case Some(error) =>
					val valuesStr = {
						if (values.hasSize > 50)
							s"${ values.view.take(25).mkString(", ") }, \n\t..., \n\t${
								values.view.takeRight(25).mkString(", ") }"
						else
							values.iterator.mkString(", ")
					}
					HandleError.duringDbQuery(
						new DBException(s"DB query failed.\nSQL: $sql\nValues:[$valuesStr]", error))
					
					// If the captured error was not rethrown, yields the previously prepared result
					lazyFinalResult.value
				
				// Case: No error encountered => Yields the result as prepared
				case None => lazyFinalResult.value
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
	
	private def setValues(statement: PreparedStatement, values: Seq[Value]) = values.iterator.zipWithIndex
		.foreach { case (value, i) =>
			Connection.sqlValueConverter(value) match {
				case Some((objectValue, jdbcType)) => statement.setObject(i + 1, objectValue, jdbcType)
				case None => statement.setNull(i + 1, Types.NULL)
			}
		}
	
	private def stringFromResult(result: ResultSet, index: Int) = Option(result.getString(index))
}
