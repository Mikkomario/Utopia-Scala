package utopia.echo.model.response.ollama

import utopia.echo.model.response.ReplyLike

import scala.concurrent.Future
import scala.util.Try

/**
  * Common trait / interface for Ollama LLM replies, whether they're streamed or buffered
  * and whether they're in chat or response format.
  * @tparam Buffered Buffered version of this response
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait OllamaReplyLike[+Buffered] extends ReplyLike[Buffered]
{
	/**
	 * @return A future that resolves into the final response statistics once they arrive.
	 *         Will contain a failure in case response-parsing / processing failed.
	 */
	def statisticsFuture: Future[Try[OllamaResponseStatistics]]
}