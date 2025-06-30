package utopia.vault.context

import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.vault.database.{ConnectionPool, Tables}

import scala.concurrent.ExecutionContext

object VaultContext
{
	// ATTRIBUTES   ----------------------
	
	private var latest: Option[VaultContext] = None
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The last constructed vault context instance.
	  *         None if no Vault context has been set up.
	  */
	private[vault] def ifInitialized = latest
	
	/**
	  * @return An implicit logging implementation, based on the last constructed VaultContext instance.
	  *         Defaults to a [[SysErrLogger]], if no context has been set up.
	  */
	private[vault] implicit def log: Logger = VaultLogger
	
	
	// OTHER    --------------------------
	
	/**
	  * @param exc Execution context to use
	  * @param connectionPool Connection pool to use
	  * @param databaseName Name of the primary database
	  * @param tables Tables being used
	  * @return A new context that wraps the specified values
	  */
	def apply(exc: ExecutionContext, connectionPool: ConnectionPool, databaseName: String, tables: Tables,
	          log: Logger = SysErrLogger): VaultContext =
		Context(exc, connectionPool, databaseName, tables, log)
	
	
	// NESTED   --------------------------
	
	private case class Context(executionContext: ExecutionContext, connectionPool: ConnectionPool, databaseName: String,
	                           tables: Tables, log: Logger)
		extends VaultContext
		
	private object VaultLogger extends Logger
	{
		override def apply(error: Option[Throwable], message: String): Unit = ifInitialized match {
			case Some(context) => context.log(error, message)
			case None => SysErrLogger(error, message)
		}
	}
}

/**
  * Common trait for interfaces that specify the commonly used values in a project that uses Vault
  * @author Mikko Hilpinen
  * @since 11/03/2024, v1.19
  */
trait VaultContext
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Implicit logging implementation used
	  */
	implicit def log: Logger
	/**
	  * @return Generally available execution context used in certain high-level asynchronous functions
	  */
	implicit def executionContext: ExecutionContext
	/**
	  * @return Connection pool used for acquiring new database connections
	  */
	implicit def connectionPool: ConnectionPool
	
	/**
	  * @return Name of the (primary) database to use
	  */
	def databaseName: String
	/**
	  * @return Access to the database tables used in this project
	  */
	def tables: Tables
	
	
	// INITIAL CODE ---------------------
	
	// Remembers that this is the last constructed context instance
	VaultContext.latest = Some(this)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param tableName Name of the targeted table
	  * @return A DB table with that name
	  */
	def table(tableName: String) = tables(databaseName, tableName)
}
