package utopia.echo.model.response.ollama

import utopia.echo.model.response.{BufferedReplyLike, TokenUsage}
import utopia.flow.async.TryFuture

import scala.concurrent.Future
import scala.util.Try

/**
 * Common trait for Ollama responses which have been fully received & buffered
 * @tparam Repr Implementing type of this trait
 * @author Mikko Hilpinen
 * @since 12.01.2025, v1.2
 */
trait BufferedOllamaReplyLike[+Repr] extends OllamaReplyLike[Repr] with BufferedReplyLike[Repr]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return Statistics concerning this response
	 */
	def statistics: OllamaResponseStatistics
	
	
	// IMPLEMENTED  --------------------
	
	override def tokenUsage: TokenUsage = statistics.tokenUsage
	override def statisticsFuture: Future[Try[OllamaResponseStatistics]] = TryFuture.success(statistics)
}
