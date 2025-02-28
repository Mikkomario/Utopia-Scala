package utopia.logos.database

import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.vault.context.{VaultContext, VaultContextWrapper}
import utopia.vault.database.{ConnectionPool, Tables}

import scala.concurrent.ExecutionContext

/**
  * An interface used for setting up database-interactions within this module
  * @author Mikko Hilpinen
  * @since 11/03/2024, v0.2
  */
object LogosContext extends VaultContextWrapper
{
	// ATTRIBUTES   ---------------------
	
	private var vaultContext: Option[VaultContext] = None
	private var _jsonParser: JsonParser = JsonReader
	private var _log: Logger = SysErrLogger
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return The logging implementation used within this project
	 */
	implicit def log: Logger = _log
	/**
	  * @return The JSON processor used in this project
	  */
	implicit def jsonParser: JsonParser = _jsonParser
	
	
	// IMPLEMENTED  ---------------------
	
	override protected def wrapped: VaultContext =
		vaultContext.getOrElse { throw new IllegalStateException("LogosContext has not been set up yet") }
		
	
	// OTHER    -------------------------
	
	/**
	  * @param vaultContext A context that provides the database interaction properties for this interface
	 * @param jsonParser The JSON-parsing implementation used in this project
	 * @param log Logging implementation used in this project. Default = write to System.err
	  */
	def setup(vaultContext: VaultContext, jsonParser: JsonParser, log: Logger) = {
		this.vaultContext = Some(vaultContext)
		_log = log
		_jsonParser = jsonParser
	}
	/**
	  * @param exc Execution context to use for asynchronous operations
	  * @param cPool Connection pool to use in order to acquire database connections
	  * @param databaseName Name of the database that contains the Logos tables
	  * @param tables The interface for accessing the tables within this project
	  * @param jsonParser The JSON-parsing implementation used in this project
	  * @param log Logging implementation used in this project. Default = write to System.err
	  */
	def setup(exc: ExecutionContext, cPool: ConnectionPool, databaseName: String, tables: Tables,
	          jsonParser: JsonParser, log: Logger = SysErrLogger): Unit =
		setup(VaultContext(exc, cPool, databaseName, tables), jsonParser, log)
}
