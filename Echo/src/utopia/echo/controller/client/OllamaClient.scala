package utopia.echo.controller.client

import utopia.annex.util.RequestResultExtensions._
import utopia.disciple.apache.Gateway
import utopia.disciple.controller.{RequestInterceptor, ResponseInterceptor}
import utopia.disciple.http.request.Timeout
import utopia.disciple.model.error.RequestFailedException
import utopia.echo.controller.Chat
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.generate.Prompt
import utopia.echo.model.request.llm.{CreateModelRequest, ListModelsRequest, ShowModelRequest}
import utopia.echo.model.response.llm.StreamedStatus
import utopia.flow.async.TryFuture
import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.equality.EqualsExtensions._
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
	extends LlmServiceClient(
		Gateway(maximumTimeout = Timeout(15.minutes, 15.minutes),
			requestInterceptors = requestInterceptors, responseInterceptors = responseInterceptors),
		serverAddress)
{
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
	
	
	// OTHER    ----------------------------
	
	/**
	 * Requests a buffered reply to a prompt.
	 * Buffered, in this context, means that the reply will be received all at once (which may take a while to complete).
	 *
	 * This function is suitable for very simple queries in a background thread,
	 * where user-interactivity is not important. For more complex interactions, consider using the [[Chat]] interface.
	 *
	 * @param prompt Prompt to send out for the LLM
	 * @param system System message to set.
	 *               If not empty, this will override whatever the targeted model's default system message is.
	 *               Default = empty = no system message.
	 * @param encodedImages Base 64 encoded images to include with the request.
	 *                      Use only with models that support image input.
	 *                      Default = empty.
	 * @param json Whether the Ollama server should process the LLM's response into a valid JSON string.
	 *             This will also include a statement, instructing the LLM to respond in JSON.
	 *
	 *             Note: If you want to perform the JSON parsing in Echo, or by yourself, set this to false and
	 *             request the LLM to respond in JSON within your prompt.
	 *
	 *             Default = false.
	 * @param llm The name of the targeted large language model (implicit)
	 * @return A future that resolves into a buffered reply from the Ollama server.
	 *         May yield a failure.
	 */
	def bufferedResponseFor(prompt: String, system: String = "", encodedImages: Seq[String] = Empty,
	                        json: Boolean = false)
	                       (implicit llm: LlmDesignator) =
		push(Prompt(prompt, systemMessage = system, encodedImages = encodedImages).toQuery.copy(requestJson = json)
			.toRequest.buffered).future
	
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
}
