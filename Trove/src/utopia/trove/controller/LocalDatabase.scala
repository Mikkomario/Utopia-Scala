package utopia.trove.controller

import java.nio.file.Path

import ch.vorburger.mariadb4j.{DB, DBConfigurationBuilder}
import utopia.flow.async.{CloseHook, Volatile, VolatileOption}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.trove.database.{DbDatabaseVersion, DbDatabaseVersions}
import utopia.trove.event.DatabaseSetupEvent.{DatabaseConfigured, DatabaseStarted, SetupFailed, SetupSucceeded, UpdateApplied, UpdateFailed, UpdatesFound}
import utopia.trove.event.{DatabaseSetupEvent, DatabaseSetupListener}
import utopia.trove.model.enumeration.DatabaseStatus
import utopia.trove.model.enumeration.DatabaseStatus.{NotStarted, Setup, Started, Starting, Stopping, Updating}
import utopia.trove.model.enumeration.SqlFileType.Full
import utopia.vault.database.{Connection, ConnectionPool}
import utopia.vault.model.immutable.Table

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
  * Used for setting up a local database
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  */
object LocalDatabase
{
	// ATTRIBUTES	---------------------------
	
	private val _statusPointer = Volatile[DatabaseStatus](NotStarted)
	private val dbPointer = VolatileOption[DB]()
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Whether this database is currently starting or stopping
	  */
	def isProcessing = _statusPointer.value.isProcessing
	
	/**
	  * A pointer to this database's status
	  */
	def statusPointer = _statusPointer.valueView
	
	
	// OTHER	-------------------------------
	
	/**
	  * Drops a versioned database
	  * @param dbName Name of the managed / targeted database
	  * @param versionTableName Name of the version table
	  * @param versionTable A version table access (call by name) (is called only if such a table already exists in db)
	  * @param exc Implicit execution context
	  * @param connectionPool Implicit connection pool
	  * @return Failure if database failed to start or if failed to clear the database
	  */
	def clear(dbName: String, versionTableName: String, versionTable: => Table)
			 (implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
	{
		// Starts the database first if not started already
		start().flatMap { _ =>
			connectionPool.tryWith { implicit connection =>
				// Clears the version table, if available (and if targeting another db)
				if (connection.existsTable(dbName, versionTableName))
					DbDatabaseVersions(versionTable).delete()
				// Drops the target database
				connection.dropDatabase(dbName)
			}
		}
	}
	
	/**
	  * Sets up the local database
	  * @param updateDirectory Directory from which update files are read
	  * @param dbName Name of the managed database
	  * @param versionTableName Name of the table that contains version data
	  * @param versionTable Version table (call by name)
	  * @param listener Listener to be informed of database setup events
	  * @param exc Implicit execution context
	  * @param connectionPool A connection pool for forming database connections
	  * @return Database setup completion event
	  */
	def setupWithListener(updateDirectory: Path, dbName: String, versionTableName: String,
						  versionTable: => Table)(listener: DatabaseSetupListener)
						 (implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
		setup(updateDirectory, dbName, versionTableName, versionTable, Some(listener))
	
	/**
	  * Sets up the local database
	  * @param updateDirectory Directory from which update files are read
	  * @param dbName Name of the managed database
	  * @param versionTableName Name of the table that contains version data
	  * @param versionTable Version table (call by name)
	  * @param listener Listener to be informed of database setup events (optional)
	  * @param exc Implicit execution context
	  * @param connectionPool A connection pool for forming database connections
	  * @return Database setup completion event
	  */
	def setup(updateDirectory: Path, dbName: String, versionTableName: String, versionTable: => Table,
			  listener: Option[DatabaseSetupListener] = None)
			 (implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
	{
		// Table is cached once it has been used once
		lazy val table = versionTable
		
		def fireEvent(event: => DatabaseSetupEvent) = listener.foreach { _.onDatabaseSetupEvent(event) }
		
		val result = start(listener).flatMap { _ =>
			// Checks current database version, and whether database has been configured at all
			connectionPool.tryWith { implicit connection =>
				if (connection.existsTable(dbName, versionTableName))
					DbDatabaseVersion(table).latest
				else
					None
			}.flatMap { currentDbVersion =>
				ScanSourceFiles(updateDirectory, currentDbVersion.map { _.number }).flatMap { sources =>
					// If no update files were available, completes
					if (sources.isEmpty)
					{
						// May create the target database first, however
						Success(
							if (currentDbVersion.isEmpty)
								connectionPool.tryWith { _.createDatabase(dbName) } match
								{
									case Success(_) => SetupSucceeded(None)
									case Failure(error) => SetupFailed(error)
								}
							else
								SetupSucceeded(currentDbVersion)
						)
					}
					else
					{
						fireEvent(UpdatesFound(sources, currentDbVersion))
						
						connectionPool.tryWith { implicit connection =>
							// Creates, recreates and/or uses the targeted database if necessary
							if (currentDbVersion.isDefined)
							{
								// If a full update will be introduced and there already exists a database version,
								// drops it and recreates it first
								if (sources.exists { _.fileType == Full })
								{
									// Backs up version data and inserts it to the new database version
									val versionsAccess = DbDatabaseVersions(table)
									val versions = versionsAccess.all
									connection.dropDatabase(dbName, checkIfExists = false)
									connection.createDatabase(dbName, checkIfExists = false)
									versionsAccess.insert(versions.map { _.data })
								}
								// In case there is an update and db exists already, uses it
								else
									connection.dbName = dbName
							}
							// If there wasn't any target database before, creates one
							else
								connection.createDatabase(dbName)
							
							// Imports the source files in order
							// Keeps track of the version(s) during the loop in order to record the final version
							var version = currentDbVersion.map { _.number }
							val updateFailure = sources.zipWithIndex.view.map { case (source, index) =>
								val result = connection.executeStatementsFrom(source.path)
								// Fires an event the update succeeded
								result match
								{
									case Success(_) =>
										version = Some(source.targetVersion)
										fireEvent(UpdateApplied(source, sources.drop(index + 1)))
										Right(())
									case Failure(error) => Left(UpdateFailed(error, source, version))
								}
							}.findMap { _.leftOption }
							
							// Records new database version
							val newVersion = version.map { v => DbDatabaseVersions(table).insert(v) }
							
							// Sets up the default database name
							Connection.modifySettings { _.copy(defaultDBName = Some(dbName)) }
							
							// Returns the completion event (without firing it)
							updateFailure match
							{
								case Some(failure) => failure
								case None => SetupSucceeded(newVersion)
							}
						}
					}
				}
			}
		}
		
		// Determines, fires and returns the final event (also updates status)
		val completionEvent = result match
		{
			case Success(event) =>
				_statusPointer.update { oldStatus =>
					event match
					{
						case _: SetupSucceeded => Setup
						case _ =>
							if (oldStatus.isStarted)
								Started
							else
								NotStarted
					}
				}
				event
			case Failure(error) =>
				_statusPointer.update { s =>
					if (s.isStarted)
						Started
					else
						NotStarted
				}
				SetupFailed(error)
		}
		fireEvent(completionEvent)
		completionEvent
	}
	
	/**
	  * @param exc Implicit execution context
	  * @return Asynchronous result, containing whether the database was successfully shut down (or was already stopped)
	  */
	def shutDownAsync()(implicit exc: ExecutionContext) =
	{
		_statusPointer.futureWhere { _.isCompleted }.map { status =>
			// Only shuts down the database if there is one and its started
			if (status.isStarted)
			{
				_statusPointer.value = Stopping
				val result = Try { dbPointer.pop().foreach { _.stop() } }
				_statusPointer.value = NotStarted
				result
			}
			else
				Success(())
		}
	}
	
	/**
	  * @param exc Implicit execution context
	  * @return Whether the database was successfully shut down (or was already stopped)
	  */
	def shutDown()(implicit exc: ExecutionContext) = shutDownAsync().waitForResult()
	
	private def waitUntilNotProcessing()(implicit exc: ExecutionContext): Unit =
	{
		if (isProcessing)
			_statusPointer.futureWhere { _.isCompleted }.waitFor()
	}
	
	private def start(listener: Option[DatabaseSetupListener] = None)(implicit exc: ExecutionContext) =
	{
		Try {
			// If currently processing another request, waits for that to complete first
			waitUntilNotProcessing()
			
			// May skip the database starting process in case there is already a started database active
			if (_statusPointer.getAndUpdate { s => if (s.notStarted) Starting else Updating }.notStarted)
			{
				// Sets up the database
				val configBuilder = DBConfigurationBuilder.newBuilder()
				configBuilder.setPort(0)
				configBuilder.setDataDir(("database": Path).absolute.toString)
				val database = DB.newEmbeddedDB(configBuilder.build()) // May throw
				dbPointer.setOne(database)
				
				// Updates Vault connection settings
				Connection.modifySettings { _.copy(
					connectionTarget = configBuilder.getURL(""),
					defaultDBName = Some("test")) }
				
				listener.foreach { _.onDatabaseSetupEvent(DatabaseConfigured) }
				
				// Starts the database (may throw)
				database.start()
				_statusPointer.value = Updating
				// Closes the database when program closes
				CloseHook.registerAction { shutDown() }
				
				listener.foreach { _.onDatabaseSetupEvent(DatabaseStarted) }
			}
		}
	}
}
