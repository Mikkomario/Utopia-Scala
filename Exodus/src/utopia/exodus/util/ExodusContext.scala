package utopia.exodus.util

import utopia.flow.generic.{DataType, EnvironmentNotSetupException}
import utopia.vault.database.ConnectionPool

import scala.concurrent.ExecutionContext

/**
  * This context object holds certain global values required by many objects in this project. It is recommended to
  * set up this object at the very beginning of the application's startup.
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
object ExodusContext
{
	// ATTRIBUTES	---------------------------------
	
	private var data: Option[Data] = None
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return Execution context used in this project (implicit)
	  * @throws EnvironmentNotSetupException If setup(...) hasn't been called yet
	  */
	implicit def executionContext: ExecutionContext = get.exc
	
	/**
	  * @return Database connection pool to use in this project (implicit)
	  * @throws EnvironmentNotSetupException If setup(...) hasn't been called yet
	  */
	implicit def connectionPool: ConnectionPool = get.connectionPool
	
	/**
	  * @return Name of the database that contains the project specific tables
	  */
	def databaseName = get.databaseName
	
	private def get = data match
	{
		case Some(data) => data
		case None => throw EnvironmentNotSetupException("ExodusContext.setup must be called before using this method")
	}
	
	
	// OTHER	--------------------------------------
	
	/**
	  * Sets up the use context
	  * @param executionContext Execution context used
	  * @param connectionPool Database connection pool used
	  * @param databaseName Name of the primary database where user data is stored
	  * @param handleErrors A function for handling thrown errors
	  */
	def setup(executionContext: ExecutionContext, connectionPool: ConnectionPool, databaseName: String)
			 (handleErrors: (Throwable, String) => Unit) =
	{
		DataType.setup()
		data = Some(Data(executionContext, connectionPool, databaseName, handleErrors))
	}
	
	/**
	  * Receives the specified error
	  * @param error An error that occurred
	  * @param message An error message
	  */
	def handleError(error: Throwable, message: String) = data.foreach { _.errorHandler(error, message) }
	
	
	// NESTED	--------------------------------------
	
	private case class Data(exc: ExecutionContext, connectionPool: ConnectionPool, databaseName: String,
							errorHandler: (Throwable, String) => Unit)
}
