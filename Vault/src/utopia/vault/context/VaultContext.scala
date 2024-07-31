package utopia.vault.context

import utopia.vault.database.{ConnectionPool, Tables}

import scala.concurrent.ExecutionContext

object VaultContext
{
	// OTHER    --------------------------
	
	/**
	  * @param exc Execution context to use
	  * @param connectionPool Connection pool to use
	  * @param databaseName Name of the primary database
	  * @param tables Tables being used
	  * @return A new context that wraps the specified values
	  */
	def apply(exc: ExecutionContext, connectionPool: ConnectionPool, databaseName: String, tables: Tables): VaultContext =
		Context(exc, connectionPool, databaseName, tables)
	
	
	// NESTED   --------------------------
	
	private case class Context(executionContext: ExecutionContext, connectionPool: ConnectionPool, databaseName: String,
	                           tables: Tables)
		extends VaultContext
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
	
	
	// OTHER    -------------------------
	
	/**
	  * @param tableName Name of the targeted table
	  * @return A DB table with that name
	  */
	def table(tableName: String) = tables(databaseName, tableName)
}
