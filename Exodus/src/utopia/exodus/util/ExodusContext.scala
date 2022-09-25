package utopia.exodus.util

import utopia.access.http.Status
import utopia.citadel.util.CitadelContext
import utopia.flow.error.EnvironmentNotSetupException
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.caching.Lazy
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.metropolis.model.enumeration.ModelStyle.Full
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
	  * @return Logger to use in this context
	  */
	def logger: Logger = data match {
		case Some(d) => d.logger
		case None => SysErrLogger
	}
	
	/**
	  * @return Execution context used in this project (implicit)
	  * @throws EnvironmentNotSetupException If setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException](".setup(...) hasn't been called yet")
	@deprecated("Please use CitadelContext to access this property", "v2.0")
	implicit def executionContext: ExecutionContext = CitadelContext.executionContext
	
	/**
	  * @return Database connection pool to use in this project (implicit)
	  * @throws EnvironmentNotSetupException If setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException](".setup(...) hasn't been called yet")
	@deprecated("Please use CitadelContext to access this property", "v2.0")
	implicit def connectionPool: ConnectionPool = CitadelContext.connectionPool
	
	/**
	  * @return Unique user id generator to use in this project (implicit)
	  * @throws EnvironmentNotSetupException If .setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException](".setup(...) hasn't been called yet")
	implicit def uuidGenerator: UuidGenerator = get.uuidGenerator
	
	/**
	  * @return Name of the database that contains the project specific tables
	  * @throws EnvironmentNotSetupException If .setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException](".setup(...) hasn't been called yet")
	@deprecated("Please use CitadelContext to access this property", "v2.0")
	def databaseName = CitadelContext.databaseName
	
	/**
	  * @return Model style to use by default on this project
	  */
	@throws[EnvironmentNotSetupException](".setup(...) hasn't been called yet")
	def defaultModelStyle = get.defaultModelStyle
	
	/**
	  * @return Email validation implementation to use. None if no implementation has been provided.
	  */
	def emailValidator = data.flatMap { _.emailValidator }
	/**
	  * @return Whether email validation is supported on this implementation
	  */
	def isEmailValidationSupported = data.exists { _.emailValidator.isDefined }
	
	/**
	  * @return Whether it is required for an user to have an email address
	  */
	def userEmailIsRequired = data.exists { _.userEmailIsRequired }
	/**
	  * @return Whether it is required for an user to have a unique user name
	  */
	def uniqueUserNamesAreRequired = !userEmailIsRequired
	
	/**
	  * @return Ids of the scopes granted to all new users. This may not (needs not) list scope ids available through
	  *         the user creation context directly
	  */
	@throws[EnvironmentNotSetupException](".setup(...) hasn't been called yet")
	def defaultUserScopeIds = get.userScopeIds.value
	
	
	@throws[EnvironmentNotSetupException]("If .setup(...) hasn't been called yet")
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
	  * @param defaultModelStyle Model style used for the responses when no model style is specified in session
	  *                          opening or the request itself (default = Full)
	  * @param uuidGenerator A generator which produce new unique user ids (default = Java's random UUID)
	  * @param emailValidator Email validation implementation, if enabled (optional)
	  * @param requireUserEmail Whether all users should be required to register (and keep) email addresses
	  *                         (default = false => allows users to omit an email address if they have
	  *                         a unique user name)
	  * @param defaultUserScopeIds Ids of the scopes available to all new users (lazy).
	  *                             The scope ids available through the user creation context are also applied and
	  *                             don't need to be listed here.
	  * @param logger Implicit logger to use
	  */
	def setup(executionContext: ExecutionContext, connectionPool: ConnectionPool, databaseName: String,
	          defaultModelStyle: ModelStyle = Full, uuidGenerator: UuidGenerator = UuidGenerator.default,
	          emailValidator: Option[EmailValidator] = None, requireUserEmail: Boolean = false)
	         (defaultUserScopeIds: => Set[Int])(implicit logger: Logger) =
	{
		CitadelContext.setup(executionContext, connectionPool, databaseName)
		Status.setup()
		data = Some(Data(defaultModelStyle, uuidGenerator, Lazy(defaultUserScopeIds), emailValidator, logger,
			requireUserEmail))
	}
	
	/**
	  * Receives the specified error
	  * @param error An error that occurred
	  * @param message An error message
	  */
	@deprecated("Please use logger instead", "v4.1")
	def handleError(error: Throwable, message: String) = logger(error, message)
	
	
	// NESTED	--------------------------------------
	
	private case class Data(defaultModelStyle: ModelStyle, uuidGenerator: UuidGenerator,
	                        userScopeIds: Lazy[Set[Int]], emailValidator: Option[EmailValidator],
	                        logger: Logger, userEmailIsRequired: Boolean)
}
