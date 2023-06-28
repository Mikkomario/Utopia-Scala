package utopia.scribe.api.util

import utopia.access.http.Status
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.Pointer
import utopia.scribe.api.controller.logging.Scribe
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.database.ConnectionPool

import scala.concurrent.ExecutionContext

/**
  * A set of settings which must be initialized before the program is used
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  */
object ScribeContext
{
	// ATTRIBUTES   ---------------------
	
	private val settingsPointer = Pointer.empty[Settings]()
	
	
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
	  * @return The name of the database used for scribe features
	  */
	def databaseName = settings.dbName
	
	def backupLogger = settingOr { _.backupLogger }(SysErrLogger)
	
	
	// OTHER    -------------------------
	
	/**
	  * Initializes this context
	  * @param exc The execution context to use
	  * @param cPool The database connection pool to use
	  * @param databaseName Name of the database used for Scribe features (default = utopia_scribe_db)
	  * @param version Current software version (default = v1.0)
	  * @param backupLogger Logging implementation to use when the Scribe logging system is not working
	  *                     (default = print to console)
	  */
	def setup(exc: ExecutionContext, cPool: ConnectionPool, databaseName: String = "utopia_scribe_db",
	          version: Version = Version(1), backupLogger: Logger = SysErrLogger) =
		settingsPointer.value = Some(Settings(exc, cPool, databaseName, version, backupLogger))
	
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
	
	private case class Settings(exc: ExecutionContext, cPool: ConnectionPool, dbName: String, version: Version,
	                            backupLogger: Logger)
}
