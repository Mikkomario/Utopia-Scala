package utopia.echo.test

import utopia.access.http.Status
import utopia.annex.util.RequestResultExtensions._
import utopia.bunnymunch.jawn.JsonBunny
import utopia.echo.controller.OllamaClient
import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.TryExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext
import scala.io.StdIn

/**
  * Sets up a testing context for Echo tests
  * @author Mikko Hilpinen
  * @since 19.09.2024, v1.1
  */
object EchoTestContext
{
	// SETUP    ----------------------
	
	// Sets up the context
	Status.setup()
	
	
	// ATTRIBUTES   ------------------
	
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Echo")
	implicit val jsonParser: JsonParser = JsonBunny
	
	// private val accessLogger = new AccessLogger(new FileLogger("Echo/data/test-output", groupDuration = 1.seconds))
	/**
	  * Commonly utilized Ollama client interface
	  */
	val client = new OllamaClient()
		// requestInterceptors = Single(accessLogger),
		// responseInterceptors = Single(accessLogger))
	
	
	// OTHER    ----------------------
	
	/**
	  * Prompts the user to select a model via the console
	  * @return The model selected by the user.
	  *         None if the selection was canceled, or if there were no models available.
	  */
	def selectModel() = {
		client.localModels.future.waitForResult().toTry.log.flatMap { llms =>
			println("Please select the LLM to use")
			StdIn.selectFrom(llms.map { llm =>
				llm.designator -> s"${ llm.name } (${ llm.sizeBytes / 100000000L / 10.0 } Gb)"
			})
		}
	}
}
