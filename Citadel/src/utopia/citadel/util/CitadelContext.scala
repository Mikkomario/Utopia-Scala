package utopia.citadel.util

import utopia.flow.error.EnvironmentNotSetupException
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.time.TimeExtensions._
import utopia.vault.database.{Connection, ConnectionPool}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * This context object holds certain global values required by many features in this project. It is recommended to
  * set up this object at the very beginning of the application's startup.
  * @author Mikko Hilpinen
  * @since 26.6.2021, v1.0
  */
object CitadelContext
{
	// ATTRIBUTES	---------------------------------
	
	private var data: Option[Data] = None
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return Execution context used in this project (implicit)
	  * @throws EnvironmentNotSetupException If setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException](".setup(...) hasn't been called yet")
	implicit def executionContext: ExecutionContext = get.exc
	/**
	  * @return Database connection pool to use in this project (implicit)
	  * @throws EnvironmentNotSetupException If setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException](".setup(...) hasn't been called yet")
	implicit def connectionPool: ConnectionPool = get.connectionPool
	/**
	  * @return Name of the database that contains the project specific tables
	  * @throws EnvironmentNotSetupException If .setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException](".setup(...) hasn't been called yet")
	def databaseName = get.databaseName
	/**
	  * @return Duration how long description roles may be cached outside of the database
	  */
	def descriptionRoleCacheDuration = data match
	{
		case Some(data) => data.descriptionRoleCacheDuration
		case None => Duration.Zero
	}
	
	@throws[EnvironmentNotSetupException]("If .setup(...) hasn't been called yet")
	private def get = data match
	{
		case Some(data) => data
		case None => throw EnvironmentNotSetupException("CitadelContext.setup must be called before using this method")
	}
	
	
	// OTHER	--------------------------------------
	
	/**
	  * Sets up the use context
	  * @param executionContext Execution context used
	  * @param connectionPool Database connection pool used
	  * @param databaseName Name of the primary database where data is stored
	  * @param descriptionRoleCacheDuration Duration how long description roles may be cached so that they don't
	  *                                     need to be read from the database. Use a lower value if you expect
	  *                                     changes in description role database data. If there are no changes ever,
	  *                                     infinite duration may be appropriate. (Default = 1 hours)
	  */
	def setup(executionContext: ExecutionContext, connectionPool: ConnectionPool, databaseName: String,
	          descriptionRoleCacheDuration: Duration = 1.hours) =
	{
		DataType.setup()
		data = Some(Data(executionContext, connectionPool, databaseName, descriptionRoleCacheDuration))
		// Sets the specified database as the default database for the Connection interface, also
		Connection.modifySettings { _.copy(defaultDBName = Some(databaseName)) }
	}
	
	
	// NESTED	--------------------------------------
	
	private case class Data(exc: ExecutionContext, connectionPool: ConnectionPool, databaseName: String,
	                        descriptionRoleCacheDuration: Duration)
}
