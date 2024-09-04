package utopia.echo.model.request.llm

import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestResult
import utopia.echo.model.response.llm.GeneralOllamaModelInfo

import scala.concurrent.Future

/**
 * A request for listing the locally available models
 *
 * @author Mikko Hilpinen
 * @since 03.09.2024, v1.1
 */
object ListModelsRequest extends GetRequest[Seq[GeneralOllamaModelInfo]]
{
	override def path: String = "tags"
	override def deprecated: Boolean = false
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Seq[GeneralOllamaModelInfo]]] =
		prepared.getMany(GeneralOllamaModelInfo)
}
