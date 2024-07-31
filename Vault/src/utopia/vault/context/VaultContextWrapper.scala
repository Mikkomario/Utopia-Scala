package utopia.vault.context
import utopia.vault.database.{ConnectionPool, Tables}

import scala.concurrent.ExecutionContext

/**
  * Common trait for items that implement the [[VaultContext]] trait by wrapping another context instance
  * @author Mikko Hilpinen
  * @since 11/03/2024, v1.19
  */
trait VaultContextWrapper extends VaultContext
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The wrapped context instance
	  */
	protected def wrapped: VaultContext
	
	
	// IMPLEMENTED  -----------------------
	
	override implicit def executionContext: ExecutionContext = wrapped.executionContext
	override implicit def connectionPool: ConnectionPool = wrapped.connectionPool
	
	override def databaseName: String = wrapped.databaseName
	override def tables: Tables = wrapped.tables
}
