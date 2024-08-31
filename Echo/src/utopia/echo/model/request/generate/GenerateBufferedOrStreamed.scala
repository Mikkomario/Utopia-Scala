package utopia.echo.model.request.generate

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.http.response.ResponseParser
import utopia.echo.controller.EchoContext
import utopia.echo.controller.parser.StreamedReplyResponseParser
import utopia.echo.model.response.generate.{BufferedReply, StreamedOrBufferedReply}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
  * A request for the Ollama API to generate a response to a prompt.
  * The response may be received in either streamed or buffered format.
  * @param params Request parameters to apply
  * @param stream Whether the response should be received as a stream (i.e. word-by-word) or all at once (false).
  *               Default = false.
  * @param exc Implicit execution context utilized in streamed response-processing
  * @param jsonParser Implicit json parser used in response-parsing
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
case class GenerateBufferedOrStreamed(params: GenerateParams, stream: Boolean = false)
                                     (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
	extends Generate[StreamedOrBufferedReply]
{
	// ATTRIBUTES   ----------------------
	
	private lazy val responseParser: ResponseParser[Response[StreamedOrBufferedReply]] = {
		// Case: Expecting a streamed response => Utilizes StreamedReplyResponseParser
		if (stream)
			new StreamedReplyResponseParser().toResponse.mapSuccess(StreamedOrBufferedReply.streamed)
		// Case: Expecting a buffered response => Parses the reply from a response model
		else
			ResponseParser.value.tryFlatMapToResponse(EchoContext.parseFailureStatus) {
				_.tryModel.map { model => StreamedOrBufferedReply.buffered(BufferedReply.fromOllamaResponse(model)) } } {
				_.getString }
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedOrBufferedReply]] =
		prepared.send(responseParser)
}
