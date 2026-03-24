package utopia.echo.test

import utopia.annex.util.RequestResultExtensions._
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.controller.Gateway
import utopia.disciple.model.request.Timeout
import utopia.echo.controller.chat.{Chat, DeepSeekChat, OllamaChat}
import utopia.echo.controller.client.{LlmServiceClient, OllamaClient}
import utopia.echo.controller.tokenization.{EstimateTokenCount, TokenCounter}
import utopia.echo.model.enumeration.DeepSeekModel
import utopia.echo.model.llm.LlmDesignator
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.util.result.TryExtensions._

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.{Failure, Success}

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
	implicit val tokenCounter: TokenCounter = EstimateTokenCount
	
	// private val accessLogger = new AccessLogger(new FileLogger("Echo/data/test-output", groupDuration = 1.seconds))
	val gateway = Gateway(maximumTimeout = Timeout(15.minutes, 15.minutes))
	// requestInterceptors = Single(accessLogger),
	// responseInterceptors = Single(accessLogger))
	
	/**
	 * Commonly utilized Ollama client interface
	 */
	lazy val ollamaClient: OllamaClient = OllamaClient.using(gateway)
	
	lazy val multiLineIndicator = "\"\"\""
	
	
	// OTHER    ----------------------
	
	/**
	 * Sets up either Ollama or DeepSeek chat
	 * @return Chat instance to use, and the available models. None if the user canceled the process.
	 */
	def setupChat() = {
		println("Which LLM service do you want to use?")
		StdIn.selectFrom(Pair(1 -> "Ollama", 2 -> "DeepSeek"))
			.flatMap[(Chat, Seq[LlmDesignator])] {
				case 1 =>
					ollamaClient.localModels.future.waitForResult().toTry
						.logWithMessage("Failed to look up the available models")
						.flatMap { models =>
							println("Please select the LLM to use")
							StdIn
								.selectFrom(models.map { llm =>
									llm.designator -> s"${ llm.name } (${ llm.sizeBytes / 100000000L / 10.0 } Gb)"
								})
								.map { implicit llm =>
									implicit val client: OllamaClient = ollamaClient
									val chat = new OllamaChat(llm)
									setupOllamaSystemMessage(chat)
									chat -> models.map { _.designator }
								}
						}
				case _ =>
					StdIn.readNonEmptyLine("Please write the API key used when accessing DeepSeek").flatMap { apiKey =>
						println("Please select the LLM to use")
						StdIn.selectFrom(DeepSeekModel.values.map { m => m -> m.llmName }).map { model =>
							val chat = new DeepSeekChat(
								LlmServiceClient.deepSeek(apiKey, gateway, maxParallelRequests = Some(1)), model)
								
							if (StdIn.ask("Do you want to specify a system message?"))
								StdIn.readNonEmptyLine("Please specify the system message to use")
									.foreach(chat.appendSystemMessage)
							
							chat -> DeepSeekModel.values
						}
					}
			}
	}
	
	/**
	  * Prompts the user to select a model via the console
	  * @return The model selected by the user.
	  *         None if the selection was canceled, or if there were no models available.
	  */
	def selectModel() = {
		ollamaClient.localModels.future.waitForResult().toTry.log.flatMap { llms =>
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
	
	private def setupOllamaSystemMessage(chat: Chat)(implicit llm: LlmDesignator) = {
		// Sets up the system message
		println("Looking up the model's system message...")
		val currentSystemMessage = ollamaClient.showModel.future.waitForResult().toTry match {
			case Success(info) => info.systemMessage
			case Failure(error) =>
				log(error, "Failed to acquire model info")
				""
		}
		val (originalSystemMessage, newSystemMessage) = {
			if (currentSystemMessage.isEmpty) {
				println("Currently there is no system message defined")
				val newSystemMessage = {
					if (StdIn.ask("Do you want to specify a system message for the duration of this session?",
						default = true))
						requestSystemMessage()
					else
						None
				}
				"" -> newSystemMessage
			}
			else {
				println(s"The current system message is: $currentSystemMessage")
				if (StdIn.ask("\nDo you want to overwrite this message?"))
					currentSystemMessage -> requestSystemMessage()
				else
					currentSystemMessage -> None
			}
		}
		
		chat.defaultSystemMessageTokensPointer.value = EstimateTokenCount.tokensIn(originalSystemMessage)
		newSystemMessage.foreach(chat.appendSystemMessage)
	}
	
	private def requestSystemMessage() =
		requestMultiLineString(
			s"Please specify the system message to use. You can write multiple lines if you start and end with $multiLineIndicator.")
}
