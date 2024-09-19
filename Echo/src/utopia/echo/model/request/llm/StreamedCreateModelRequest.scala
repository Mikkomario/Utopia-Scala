package utopia.echo.model.request.llm

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.controller.parser.StreamedStatusParser
import utopia.echo.model.request.llm.CreateModelRequest.CreateModelRequestFactory
import utopia.echo.model.response.llm.StreamedStatus
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import java.nio.file.Path
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object StreamedCreateModelRequest
{
	// COMPUTED ------------------------------
	
	/**
	  * @param exc Implicit execution context
	  * @param jsonParser Implicit json parser used
	  * @param log Implicit logging implementation used
	  * @return A new factory for constructing requests
	  */
	def factory(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger) =
		new StreamedCreateModelRequestFactory()
		
	
	// IMPLICIT ------------------------------
	
	implicit def objectToFactory(@unused o: StreamedCreateModelRequest.type)
	                            (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger): StreamedCreateModelRequestFactory =
		factory
	
	
	// NESTED   ------------------------------
	
	class StreamedCreateModelRequestFactory(override val deprecationCondition: Option[View[Boolean]] = None)
	                                       (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
		extends CreateModelRequestFactory[StreamedCreateModelRequest, StreamedCreateModelRequestFactory]
	{
		override protected def apply(name: String, modelFile: Either[Path, String],
		                             deprecationView: View[Boolean]): StreamedCreateModelRequest =
			StreamedCreateModelRequest(name, modelFile, deprecationView)
		
		override def withDeprecationCondition(condition: View[Boolean]): StreamedCreateModelRequestFactory =
			new StreamedCreateModelRequestFactory(Some(condition))
	}
}

/**
  * A request for creating a new (local) Ollama model.
  * Receives the response as a streamed status.
  * @author Mikko Hilpinen
  * @since 19.09.2024, v1.1
  */
case class StreamedCreateModelRequest(name: String, modelFile: Either[Path, String],
                                      deprecationView: View[Boolean] = AlwaysFalse)
                                     (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
	extends CreateModelRequest[StreamedStatus]
{
	// IMPLEMENTED  ---------------------------
	
	override def stream: Boolean = true
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedStatus]] =
		prepared.send(new StreamedStatusParser().toResponse)
}