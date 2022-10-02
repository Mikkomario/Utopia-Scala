package utopia.journey.util

import utopia.flow.error.EnvironmentNotSetupException
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.json.JsonParser

import java.nio.file.Path
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
  * Settings used globally within the Journey project, and possibly in its sub-projects as well
  * @author Mikko Hilpinen
  * @since 11.7.2020, v0.1
  */
object JourneyContext
{
	// ATTRIBUTES	---------------------------
	
	private var settings: Try[Settings] = Failure(EnvironmentNotSetupException(
		"JourneySettings.setup(...) must be called before accessing said settings"))
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Logger to use in recoding encountered errors
	  */
	implicit def logger: Logger = settings match {
		case Success(s) => s.logger
		case _ => SysErrLogger
	}
	
	/**
	  * @return Json parser configured in these settings
	  * @throws EnvironmentNotSetupException if setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException]("These settings haven't been initialized yet. Please call .setup(...) first")
	implicit def jsonParser: JsonParser = settings.get.jsonParser
	
	/**
	  * @return Execution context used for handling background operations & threads
	  * @throws EnvironmentNotSetupException if setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException]("These settings haven't been initialized yet. Please call .setup(...) first")
	implicit def executionContext: ExecutionContext = settings.get.exc
	
	/**
	  * @return Directory where locally generated .json files should be stored
	  * @throws EnvironmentNotSetupException if setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException]("These settings haven't been initialized yet. Please call .setup(...) first")
	def containersDirectory = settings.get.containersDirectory
	
	/**
	  * @return Directory where locally generated .json files for persisted requests should be stored
	  * @throws EnvironmentNotSetupException if setup(...) hasn't been called yet
	  */
	@throws[EnvironmentNotSetupException]("These settings haven't been initialized yet. Please call .setup(...) first")
	def requestsDirectory = containersDirectory/"requests"
	
	
	// OTHER	-------------------------------
	
	/**
	  * Sets up these settings. Will overwrite any existing settings if setup was already called before.
	  * @param containersDirectory Directory where locally generated .json files will be stored (default = ./data)
	  * @param exc Execution context used in handling background operation threads
	  * @param jsonParser Parser used when parsing json data
	  */
	def setup(containersDirectory: Path = "data")
	         (implicit exc: ExecutionContext, jsonParser: JsonParser, logger: Logger) =
	{
		DataType.setup()
		settings = Success(Settings(executionContext, jsonParser, logger, containersDirectory))
	}
	
	/**
	  * Records an error
	  * @param error Error that occurred
	  * @param message Additional error message (optional)
	  */
	@deprecated("Please use .logger instead", "v0.2")
	def log(error: Throwable, message: String = "") = logger(error, message)
	
	/**
	  * Records an error message
	  * @param message An error message
	  */
	@deprecated("Please use .logger instead", "v0.2")
	def log(message: String) = logger(message)
	
	private def defaultErrorHandler(error: Option[Throwable], message: String) =
	{
		if (message.nonEmpty)
			println(message)
		error.foreach { _.printStackTrace() }
	}
	
	
	// NESTED	-------------------------------
	
	private case class Settings(exc: ExecutionContext, jsonParser: JsonParser, logger: Logger,
	                            containersDirectory: Path)
}
