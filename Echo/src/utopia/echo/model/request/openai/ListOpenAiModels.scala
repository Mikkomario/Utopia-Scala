package utopia.echo.model.request.openai

import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestResult
import utopia.echo.model.response.openai.OpenAiModelInfo
import utopia.flow.generic.model.immutable.Model

import scala.concurrent.Future

/**
 * A request for listing (Open AI) models
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
object ListOpenAiModels extends GetRequest[Seq[OpenAiModelInfo]]
{
	// ATTRIBUTES   ----------------------
	
	override val path: String = "models"
	override val pathParams: Model = Model.empty
	override val deprecated: Boolean = false
	
	
	// IMPLEMENTED  ---------------------
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Seq[OpenAiModelInfo]]] =
		prepared.getMany(OpenAiModelInfo)
}
