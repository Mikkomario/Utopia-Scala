package utopia.echo.controller

import utopia.access.http.{Headers, Status}
import utopia.annex.controller.{ApiClient, PreparingResponseParser, QueueSystem, RequestQueue}
import utopia.annex.model.request.RequestQueueable
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.annex.util.RequestResultExtensions._
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.apache.Gateway
import utopia.disciple.controller.{RequestInterceptor, ResponseInterceptor}
import utopia.disciple.http.request.{Body, StringBody}
import utopia.disciple.http.response.ResponseParser
import utopia.disciple.model.error.RequestFailedException
import utopia.echo.model.LlmDesignator
import utopia.echo.model.request.llm.{CreateModelRequest, ListModelsRequest, ShowModelRequest}
import utopia.echo.model.response.llm.StreamedStatus
import utopia.flow.async.TryFuture
import utopia.flow.async.context.ActionQueue
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * A client-side interface for interacting with an Ollama API
  * @author Mikko Hilpinen
  * @since 11.07.2024, v1.0
  */
class OllamaClient(serverAddress: String = "http://localhost:11434/api",
                   requestInterceptors: Seq[RequestInterceptor] = Empty,
                   responseInterceptors: Seq[ResponseInterceptor] = Empty)
                  (implicit log: Logger, exc: ExecutionContext)
	extends RequestQueue
{
	// ATTRIBUTES   ------------------------
	
	private lazy val queueSystem = new QueueSystem(OllamaApiClient, 5.minutes, minOfflineDelay = 10.seconds)
	private lazy val queue = RequestQueue(queueSystem)
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return A future / action which resolves into a list of locally available models, if successful
	 */
	def localModels = push(ListModelsRequest)
	/**
	  * Retrieves model information
	  * @param llm Targeted LLM (implicit)
	  * @return Future that resolves into the implicitly targeted model's information, if successful
	  */
	def showModel(implicit llm: LlmDesignator) = push(ShowModelRequest())
	
	
	// IMPLEMENTED  ------------------------
	
	override def push[A](request: RequestQueueable[A]): ActionQueue.QueuedAction[RequestResult[A]] = queue.push(request)
	
	
	// OTHER    ----------------------------
	
	/**
	  * Makes sure that a system message is not defined in a model-file
	  * @param nameOfNewModel Name of the new model version.
	  *                       Call-by-name, only called if the targeted model's model-file contains a system message
	  *                       and the creation of a new model is therefore required.
	  * @param llm Targeted LLM (implicit)
	  * @return Future that resolves into the designator of a model
	  *         where the model-file doesn't specify a system message
	  *         (i.e. either 'llm' if that model didn't have a system message, or a new model with name 'nameOfNewModel').
	  *         May yield a failure.
	  */
	def ensureEmptySystemMessage(nameOfNewModel: => String)(implicit llm: LlmDesignator) =
		showModel.future.tryFlatMapSuccess { modelInfo =>
			if (modelInfo.systemMessage.isEmpty)
				TryFuture.success(llm)
			else {
				val modelName = nameOfNewModel
				push(CreateModelRequest.buffered(modelName, s"FROM $llm\nSYSTEM"))
					.map { _.toTry.flatMap { status =>
						if (status ~== StreamedStatus.expectedFinalStatus)
							Success(LlmDesignator(modelName))
						else
							Failure(new RequestFailedException(s"Expected status \"success\", got \"$status\""))
					} }
			}
		}
	
	
	// NESTED   ----------------------------
	
	private object OllamaApiClient extends ApiClient
	{
		// ATTRIBUTES   -----------------------
		
		override protected lazy val gateway = Gateway(
			requestInterceptors = requestInterceptors, responseInterceptors = responseInterceptors)
		
		override lazy val valueResponseParser: ResponseParser[Response[Value]] =
			ResponseParser.value.unwrapToResponse(responseParseFailureStatus) { _.getString }
		override lazy val emptyResponseParser: ResponseParser[Response[Unit]] =
			PreparingResponseParser.onlyRecordFailures(ResponseParser.stringOrLog)
		
		
		// IMPLEMENTED  -----------------------
		
		override protected implicit def jsonParser: JsonParser = JsonBunny
		override protected implicit def log: Logger = OllamaClient.this.log
		override protected implicit def exc: ExecutionContext = OllamaClient.this.exc
		
		override protected def rootPath: String = serverAddress
		
		override protected def responseParseFailureStatus: Status = EchoContext.parseFailureStatus
		
		override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
		override protected def modifyOutgoingHeaders(original: Headers): Headers = original
	}
}
