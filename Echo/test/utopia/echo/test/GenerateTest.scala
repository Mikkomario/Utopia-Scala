package utopia.echo.test

import utopia.access.http.Status
import utopia.annex.model.response.{RequestFailure, Response}
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.controller.AccessLogger
import utopia.echo.controller.OllamaClient
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.generate.{GenerateBufferedOrStreamed, Prompt, Query}
import utopia.echo.model.response.generate.StreamedOrBufferedReply
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.parse.json.JsonParser
import utopia.flow.test.TestContext._
import utopia.flow.util.logging.SysErrLogger

/**
  * A test for the generate interface
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
object GenerateTest extends App
{
	Status.setup()
	
	private implicit val jsonParser: JsonParser = JsonBunny
	private implicit val model: LlmDesignator = "wizard-vicuna-uncensored"
	
	private val accessLogger = new AccessLogger(SysErrLogger)
	private val client = new OllamaClient(
		requestInterceptors = Single(accessLogger),
		responseInterceptors = Single(accessLogger))
	private val prompt1 = Prompt("Define the word \"echo\"")
	
	println(s"Sending out: $prompt1")
	client.push(GenerateBufferedOrStreamed(Query(prompt1), stream = true)).waitFor().get match {
		case Response.Success(reply: StreamedOrBufferedReply, status, headers) =>
			println(s"Received response with status $status and headers $headers")
			println(s"Reply: ")
			reply.textPointer.addListenerAndSimulateEvent("") { change =>
				print(change.newValue.drop(change.oldValue.length))
			}
			
			val statistics = reply.statisticsFuture.waitForResult().get
			println(s"\n\nReply completed!\nStatistics: $statistics")
		
		case f: RequestFailure => throw f.cause
	}
}
