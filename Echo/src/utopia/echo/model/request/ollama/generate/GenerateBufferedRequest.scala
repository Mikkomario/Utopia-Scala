package utopia.echo.model.request.ollama.generate

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.response.ollama.BufferedOllamaReply

import scala.concurrent.Future

/**
  * A request for the Ollama API to generate a reply to a query. Buffers the whole response before returning it.
  * @param params Request parameters to apply
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
case class GenerateBufferedRequest(params: GenerateParams) extends GenerateRequest[BufferedOllamaReply]
{
	override def stream: Boolean = false
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedOllamaReply]] =
		prepared.mapModel(BufferedOllamaReply.fromOllamaGenerateResponse)
}
