package utopia.trove.controller

import java.nio.file.Path

import ch.vorburger.mariadb4j.{DB, DBConfigurationBuilder}
import utopia.flow.async.{CloseHook, Volatile, VolatileOption}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.event.{ChangeListener, Changing}
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
	
	/**
	  * A pointer to this database's status
	  */
	lazy val statusPointer: Changing[DatabaseStatus] = new StatusView()
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Whether this database is currently starting or stopping
	  */
	def isProcessing = _statusPointer.get.isProcessing
	
	
	// OTHER	-------------------------------
	
	// TODO: Continue
	/*
	  * Clears existing data from the database
	  * @return Success or failure
	  */
		/*
	def clear() =  Try {
		// Sets up the database
		val configBuilder = DBConfigurationBuilder.newBuilder()
		configBuilder.setPort(0)
		configBuilder.setDataDir(("database": Path).absolute.toString)
		val database = DB.newEmbeddedDB(configBuilder.build()) // May throw
		
		// Updates Vault connection settings
		Connection.modifySettings { _.copy(
			connectionTarget = configBuilder.getURL(""),
			defaultDBName = Some("test")) }
		
		// Starts the database (may throw)
		database.start()
		// Closes the database when program closes
		CloseHook.registerAction {
			println("Stopping database")
			database.stop()
		}
		
		// Clears old data
		connectionPool { _.dropDatabase(Tables.databaseName) }
	}*/
	
	/**
	  * Sets up the local database
	  * @param updateDirectory Directory from which update files are read
	  * @param versionDbName Name of the database that contains version data
	  * @param versionTableName Name of the table that contains version data
	  * @param versionTable Version table (call by name)
	  * @param targetDatabaseName Name of the database targeted by the updates and used by the program.
	  *                           Empty if not specified (default).
	  * @param listener Listener to be informed of database setup events (optional)
	  * @param exc Implicit execution context
	  * @param connectionPool A connection pool for forming database connections
	  * @return Database setup completion event
	  */
	def setup(updateDirectory: Path, versionDbName: String, versionTableName: String, versionTable: => Table,
			  targetDatabaseName: String = "", listener: Option[DatabaseSetupListener] = None)
			 (implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
	{
		// Table is cached once it has been used once
		lazy val table = versionTable
		
		def fireEvent(event: => DatabaseSetupEvent) = listener.foreach { _.onDatabaseSetupEvent(event) }
		
		val result = Try {
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
				
				fireEvent(DatabaseConfigured)
				
				// Starts the database (may throw)
				database.start()
				_statusPointer.set(Updating)
				// Closes the database when program closes
				CloseHook.registerAction { shutDown() }
				
				fireEvent(DatabaseStarted)
			}
			
		}.flatMap { _ =>
			// Checks current database version, and whether database has been configured at all
			connectionPool.tryWith { implicit connection =>
				if (connection.existsTable(versionDbName, versionTableName))
					DbDatabaseVersion(table).latest
				else
					None
			}.flatMap { currentDbVersion =>
				ScanSourceFiles(updateDirectory, currentDbVersion.map { _.number }).flatMap { sources =>
					// If no update files were available, completes
					if (sources.isEmpty)
						Success(SetupSucceeded(currentDbVersion))
					else
					{
						fireEvent(UpdatesFound(sources, currentDbVersion))
						
						connectionPool.tryWith { implicit connection =>
							// Drops or uses the targeted database if necessary
							if (targetDatabaseName.nonEmpty && currentDbVersion.isDefined)
							{
								if (sources.exists { _.fileType == Full })
									connection.dropDatabase(targetDatabaseName)
								else
									connection.dbName = targetDatabaseName
							}
							
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
							if (targetDatabaseName.nonEmpty)
								Connection.modifySettings { _.copy(defaultDBName = Some(targetDatabaseName)) }
							
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
				_statusPointer.set(Stopping)
				val result = Try { dbPointer.pop().foreach { _.stop() } }
				_statusPointer.set(NotStarted)
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
	
	
	// NESTED	--------------------------
	
	private class StatusView extends Changing[DatabaseStatus]
	{
		override def value = _statusPointer.get
		override def listeners = _statusPointer.listeners
		override def listeners_=(newListeners: Vector[ChangeListener[DatabaseStatus]]) =
			_statusPointer.listeners = newListeners
	}
}
