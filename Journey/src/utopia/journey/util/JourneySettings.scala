package utopia.journey.util

import java.nio.file.Path

import utopia.flow.async.ThreadPool
import utopia.flow.generic.{DataType, EnvironmentNotSetupException}
import utopia.flow.parse.{JSONReader, JsonParser}
import utopia.flow.util.FileExtensions._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
  * Settings used globally within the Journey project, and possibly in its sub-projects as well
  * @author Mikko Hilpinen
  * @since 11.7.2020, v1
  */
object JourneySettings
{
	// ATTRIBUTES	---------------------------
	
	private var settings: Try[Settings] = Failure(EnvironmentNotSetupException(
		"JourneySettings.setup(...) must be called before accessing said settings"))
	
	
	// COMPUTED	-------------------------------
	
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
	
	
	// OTHER	-------------------------------
	
	/**
	  * Sets up these settings. Will overwrite any existing settings if setup was already called before.
	  * @param containersDirectory Directory where locally generated .json files will be stored (default = ./data)
	  * @param jsonParser Parser used when parsing json data (default = JSONReader, which performs sub-optimally
	  *                   but doesn't require any imports)
	  * @param executionContext Execution context used in handling background operation threads
	  *                         (default = new thread pool with default settings)
	  */
	def setup(containersDirectory: Path = "data", jsonParser: JsonParser = JSONReader,
			  executionContext: ExecutionContext = new ThreadPool("Journey").executionContext) =
	{
		DataType.setup()
		settings = Success(Settings(executionContext, jsonParser, containersDirectory))
	}
	
	
	// NESTED	-------------------------------
	
	private case class Settings(exc: ExecutionContext, jsonParser: JsonParser, containersDirectory: Path)
}
