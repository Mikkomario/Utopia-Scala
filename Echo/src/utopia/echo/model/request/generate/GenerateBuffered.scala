package utopia.echo.model.request.generate

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.response.generate.BufferedReply

import scala.concurrent.Future

/**
  * A request for the Ollama API to generate a reply to a query. Buffers the whole response before returning it.
  * @param params Request parameters to apply
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
case class GenerateBuffered(params: GenerateParams) extends Generate[BufferedReply]
{
	override def stream: Boolean = false
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedReply]] =
		prepared.mapModel(BufferedReply.fromOllamaResponse)
}
