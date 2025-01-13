package utopia.echo.model.response.generate

import utopia.echo.model.response.{BufferedOllamaResponse, BufferedOllamaResponseLike, ResponseStatistics}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now

import java.time.Instant

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
case class BufferedReply(text: String, statistics: ResponseStatistics, lastUpdated: Instant = Now)
	extends BufferedOllamaResponse with BufferedOllamaResponseLike[BufferedReply] with Reply
{
	// IMPLEMENTED  -----------------------
	
	override def self: BufferedReply = this
}
