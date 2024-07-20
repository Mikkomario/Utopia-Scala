package utopia.echo.model.response

import utopia.annex.model.manifest.SchrodingerState.Alive
import utopia.flow.async.TryFuture
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

object BufferedReply
{
	// COMPUTED -----------------------
	
	/**
	  * @return An empty reply
	  */
	def empty = apply("", ResponseStatistics.empty, Now)
	
	
	// OTHER    -----------------------
	
	/**
	  * Converts an Ollama API -originated response model into a buffered reply.
	  * Should only be called for models where "done" is set to true (i.e. for the final responses).
	  * @param responseModel A response model returned by the Ollama API
	  * @return A buffered reply (final) parsed from the response
	  */
	def fromOllamaResponse(responseModel: Model) = apply(responseModel("response").getString,
		ResponseStatistics.fromOllamaResponse(responseModel), responseModel("created_at").getInstant)
}

/**
  * A reply from an LLM that has been completely read
  * @param text Text contents of this reply
  * @param statistics Statistics concerning this reply
  * @param lastUpdated Origin time of this reply
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  */
case class BufferedReply(text: String, statistics: ResponseStatistics, lastUpdated: Instant = Now) extends Reply
{
	// IMPLEMENTED  -----------------------
	
	override def future: Future[Try[BufferedReply]] = TryFuture.success(this)
	override def statisticsFuture: Future[Try[ResponseStatistics]] = TryFuture.success(statistics)
	
	override def textPointer: Changing[String] = Fixed(text)
	override def lastUpdatedPointer: Changing[Instant] = Fixed(lastUpdated)
	
	override def state = Alive
}
