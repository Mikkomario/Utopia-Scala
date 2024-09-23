package utopia.echo.model.request.llm

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.http.request.Body
import utopia.echo.controller.parser.StreamedPullResponseParser
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.response.llm.StreamedPullStatus
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.{ExecutionContext, Future}

object StreamedPullRequest
{
	/**
	  * Creates a new request for pulling LLMs into local Ollama
	  * @param deprecationView A view that contains true if this request becomes deprecated.
	  *                        Deprecated requests are not sent.
	  *                        If deprecation occurs after this request has been sent, it is ignored.
	  *                        Default = never.
	  * @param llm Implicit targeted LLM
	  * @param exc Implicit execution context used in response-parsing
	  * @param jsonParser Implicit json parser used in response-parsing
	  * @param log Implicit logging implementation used in response-parsing & generated pointers
	  * @return A new request
	  */
	def apply(deprecationView: View[Boolean] = AlwaysFalse)
	         (implicit llm: LlmDesignator, exc: ExecutionContext, jsonParser: JsonParser, log: Logger) =
		new StreamedPullRequest(deprecationView)
	
	/**
	  * Creates a new request for pulling LLMs into local Ollama
	  * @param deprecationCondition A call-by-name function that yields true if this request becomes deprecated.
	  *                        Deprecated requests are not sent.
	  *                        If deprecation occurs after this request has been sent, it is ignored.
	  *                        Default = never.
	  * @param llm Implicit targeted LLM
	  * @param exc Implicit execution context used in response-parsing
	  * @param jsonParser Implicit json parser used in response-parsing
	  * @param log Implicit logging implementation used in response-parsing & generated pointers
	  * @return A new request
	  */
	def deprecatingIf(deprecationCondition: => Boolean)
	                 (implicit llm: LlmDesignator, exc: ExecutionContext, jsonParser: JsonParser, log: Logger) =
		apply(View(deprecationCondition))
}

/**
  * A request that prompts the Ollama server to pull an LLM from a remote server.
  * The response is returned in a streaming format,
  * where the status & download progress updates in more or less real time.
  * @author Mikko Hilpinen
  * @since 03.09.2024, v1.1
  */
class StreamedPullRequest(deprecationView: View[Boolean] = AlwaysFalse)
                         (implicit llm: LlmDesignator, exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
	extends ApiRequest[StreamedPullStatus]
{
	// ATTRIBUTES   -------------------------
	
	private lazy val parser = new StreamedPullResponseParser().toResponse
	
	
	// IMPLEMENTED  -------------------------
	
	override def method: Method = Post
	override def path: String = "pull"
	
	override def body: Either[Value, Body] = Left(Model.from("name" -> llm.llmName, "stream" -> true))
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedPullStatus]] =
		prepared.send(parser)
}
