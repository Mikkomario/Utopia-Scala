package utopia.scribe.api.util

import utopia.access.http.Status
import utopia.flow.util.Version
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
	implicit def version: Version = settings.version
	
	
	// COMPUTED -------------------------
	
	private def settings = settingsPointer.value
		.getOrElse { throw new IllegalStateException("ScribeContext hasn't been initialized yet") }
	
	/**
	  * @return The name of the database used for scribe features
	  */
	def databaseName = settings.dbName
	
	
	// OTHER    -------------------------
	
	/**
	  * Initializes this context
	  * @param exc The execution context to use
	  * @param cPool The database connection pool to use
	  * @param databaseName Name of the database used for Scribe features (default = utopia_scribe_db)
	  */
	def setup(exc: ExecutionContext, cPool: ConnectionPool, databaseName: String = "utopia_scribe_db",
	          version: Version = Version(1)) =
		settingsPointer.value = Some(Settings(exc, cPool, databaseName, version))
	
	/**
	  * Creates a new specific logger
	  * @param context The context of this logger
	  * @param severity The default severity of logged errors (optional)
	  * @param details Default details for issue variants (optional)
	  * @return A new logging implementation
	  */
	def logger(context: String, severity: Severity = Severity.default, details: String = "") =
		Scribe(context, severity, details)
	
	
	// NESTED   -------------------------
	
	private case class Settings(exc: ExecutionContext, cPool: ConnectionPool, dbName: String, version: Version)
}