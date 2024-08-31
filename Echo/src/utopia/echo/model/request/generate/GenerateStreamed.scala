package utopia.echo.model.request.generate

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.controller.parser.StreamedReplyResponseParser
import utopia.echo.model.response.generate.StreamedReply
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
  * A request for the Ollama API to generate a response to a specific query.
  * Processes the reply in a streamed fashion, updating the response text as it arrives.
  * Not suitable for JSON-based requests.
  * @param params Request parameters to use
  * @param exc Implicit execution context utilized in streamed response-processing
  * @param jsonParser Implicit json parser used in response-parsing
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
case class GenerateStreamed(params: GenerateParams)(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
	extends Generate[StreamedReply]
{
	// ATTRIBUTES   -----------------------------
	
	private lazy val responseParser = new StreamedReplyResponseParser().toResponse
	
	
	// IMPLEMENTED  -----------------------------
	
	override def stream: Boolean = true
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedReply]] =
		prepared.send(responseParser)
}
