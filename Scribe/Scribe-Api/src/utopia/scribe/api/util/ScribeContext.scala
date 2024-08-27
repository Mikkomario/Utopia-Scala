package utopia.scribe.api.util

import utopia.access.http.Status
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.Pointer
import utopia.scribe.api.controller.logging.Scribe
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.database.{ConnectionPool, Tables}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * A set of settings which must be initialized before the program is used
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  */
object ScribeContext
{
	// ATTRIBUTES   ---------------------
	
	private val settingsPointer = Pointer.empty[Settings]
	
	
	// INITIAL CODE ---------------------
	
	// Initializes the http status classes
	Status.setup()
	
	
	// IMPLICIT -------------------------
	
	/**
	  * @return Execution context used for Scribe features
	  */
	implicit def exc: ExecutionContext = settings.exc
	/**
	  * @return Database connection pool used for Scribe features
	  */
	implicit def connectionPool: ConnectionPool = settings.cPool
	
	/**
	  * @return Program version in use
	  */
	implicit def version: Version = settingOr { _.version } { Version(1) }
	
	
	// COMPUTED -------------------------
	
	private def settings = settingsPointer.value
		.getOrElse { throw new IllegalStateException("ScribeContext hasn't been initialized yet") }
	
	/**
	  * @param tableName Name of the targeted table
	  * @return That table
	  */
	def table(tableName: String) = settings.tables(databaseName, tableName)
	/**
	  * @return The name of the database used for scribe features
	  */
	def databaseName = settings.dbName
	
	/**
	  * @return Logging implementation to use when this system is not available.
	  *         Also logs errors within this system.
	  */
	def backupLogger = settingOr { _.backupLogger }(SysErrLogger)
	
	/*
	  * @return Maximum amount of logging entries to allow within a specific time period,
	  *         and the duration within which this counter should be reset.
	  *         None if maximum logging count should not be observed.
	  */
	// def maxLoggingVelocity = settingOr { _.maxLoggingVelocity }(None)
	
	
	// OTHER    -------------------------
	
	/**
	  * Initializes this context
	  * @param exc The execution context to use
	  * @param cPool The database connection pool to use
	  * @param tables The root Tables instance that should be used when accessing database tables
	  * @param databaseName Name of the database used for Scribe features (default = utopia_scribe_db)
	  * @param version Current software version (default = v1.0)
	  * @param backupLogger Logging implementation to use when the Scribe logging system is not working
	  *                     (default = print to console)
	  * @param maximumLoggingVelocity Maximum number of logging entries and the time period within which those
	  *                               entries must occur.
	  *                               Use this value for preventing cyclical logging in some error situations,
	  *                               avoiding filling the database with logging entries.
	  *                               Logging systems may stop function if this threshold is reached.
	  *
	  *                               E.g. If specified as (1000, 1.hours), this system will stop functioning if
	  *                               1000 or more logging entries are received within a one hour time period.
	  *                               The counter would reset every hour (unless reached).
	  *
	  *                               None if no there shouldn't be any maximum (default).
	  *                               Please note that this may result in huge amount of log entries under certain
	  *                               circumstances.
	  */
	def setup(exc: ExecutionContext, cPool: ConnectionPool, tables: Tables, databaseName: String = "utopia_scribe_db",
	          version: Version = Version(1), backupLogger: Logger = SysErrLogger,
	          maximumLoggingVelocity: Option[(Int, Duration)] = None) =
	{
		settingsPointer.value = Some(Settings(exc, cPool, tables, databaseName, version, backupLogger/*,
			maximumLoggingVelocity*/))
		// Sets up maximum logging limit, also
		maximumLoggingVelocity.foreach { case (maxLogCount, resetInterval) =>
			Scribe.setupLoggingLimit(maxLogCount, resetInterval)
		}
	}
	
	/**
	  * Creates a new specific logger
	  * @param context The context of this logger
	  * @param severity The default severity of logged errors (optional)
	  * @param details Default details for issue variants (optional)
	  * @return A new logging implementation
	  */
	def scribe(context: String, severity: Severity = Severity.default, details: Model = Model.empty) =
		Scribe(context, severity, details)
	
	private def settingOr[A](extract: Settings => A)(default: => A) = settingsPointer.value match {
		case Some(s) => extract(s)
		case None => default
	}
	
	
	// NESTED   -------------------------
	
	private case class Settings(exc: ExecutionContext, cPool: ConnectionPool, tables: Tables, dbName: String,
	                            version: Version, backupLogger: Logger/*,
	                            maxLoggingVelocity: Option[(Int, Duration)]*/)
}
