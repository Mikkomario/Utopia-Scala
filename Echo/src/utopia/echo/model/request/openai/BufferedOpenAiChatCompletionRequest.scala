package utopia.echo.model.request.openai

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.enumeration.ReasoningEffort
import utopia.echo.model.request.ChatParams
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.echo.model.settings.ModelSettings
import utopia.flow.generic.model.immutable.Model
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future

object BufferedOpenAiChatCompletionRequest
{
	// OTHER    ---------------------------
	
	/**
	 * @param params Applied parameters
	 * @param reasoningEffort Reasoning effort to apply. Default = None = default.
	 * @param deprecationView A view that contains true when this request should be retracted. Default = always false.
	 * @return A new request
	 */
	def apply(params: ChatParams, reasoningEffort: Option[ReasoningEffort] = None,
	          deprecationView: View[Boolean] = AlwaysFalse): BufferedOpenAiChatCompletionRequest =
		_BufferedOpenAiChatCompletionRequest(params, reasoningEffort, deprecationView)
	
	
	// NESTED   ---------------------------
	
	private case class _BufferedOpenAiChatCompletionRequest(params: ChatParams,
	                                                        reasoningEffort: Option[ReasoningEffort],
	                                                        deprecationView: View[Boolean])
		extends BufferedOpenAiChatCompletionRequest
	{
		override def deprecated: Boolean = deprecationView.value
		
		override def withSettings(settings: ModelSettings): BufferedOpenAiChatCompletionRequest =
			copy(params = params.withSettings(settings))
		
		override protected def finalizeBody(body: Model): Model = body
		
		override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedOpenAiReply]] =
			prepared.getOne(BufferedOpenAiReply)
	}
}

/**
 * Common trait for buffered requests for Open AI chat completions
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.4.1
 */
trait BufferedOpenAiChatCompletionRequest
	extends BufferedOpenAiChatCompletionRequestLike[BufferedOpenAiReply, BufferedOpenAiChatCompletionRequest]
