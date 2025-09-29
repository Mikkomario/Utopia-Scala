package utopia.echo.test

import utopia.annex.util.RequestResultExtensions._
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.controller.Gateway
import utopia.disciple.model.request.Timeout
import utopia.echo.controller.client.OllamaClient
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
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
	// ATTRIBUTES   ------------------
	
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Echo")
	implicit val jsonParser: JsonParser = JsonBunny
	
	// private val accessLogger = new AccessLogger(new FileLogger("Echo/data/test-output", groupDuration = 1.seconds))
	val gateway = Gateway(maximumTimeout = Timeout(15.minutes, 15.minutes))
	// requestInterceptors = Single(accessLogger),
	// responseInterceptors = Single(accessLogger))
	/**
	 * Commonly utilized Ollama client interface
	 */
	implicit val client: OllamaClient = OllamaClient.using(gateway)
	
	lazy val multiLineIndicator = "\"\"\""
	
	
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
	
	def requestMultiLineString(prompt: String = ""): Option[String] = {
		StdIn.readNonEmptyLine(prompt).map { firstLine =>
			// Case: Multi-line input
			if (firstLine.startsWith(multiLineIndicator)) {
				// Case: Single-line multi-line input => Removes the multi-line indicators
				if (firstLine.endsWith(multiLineIndicator))
					firstLine.drop(multiLineIndicator.length).dropRight(multiLineIndicator.length)
				// Case: Actual multi-line input => Reads until user ends with """
				else {
					val moreLines = Iterator.continually { StdIn.readLine() }
						.takeTo { _.endsWith(multiLineIndicator) }
						.toOptimizedSeq
					((firstLine.drop(multiLineIndicator.length) +: moreLines.dropRight(1)) :+
						moreLines.last.dropRight(multiLineIndicator.length))
						.dropWhile { _.isEmpty }.dropRightWhile { _.isEmpty }
						.mkString("\n")
				}
			}
			// Case: Single-line input
			else
				firstLine
		}
	}
}
