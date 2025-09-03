package utopia.echo.model.response.ollama.generate

import utopia.echo.model.response.ollama.{BufferedOllamaResponse, BufferedOllamaResponseLike, OllamaResponseStatistics}
import utopia.echo.util.ReplyParseUtils
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now

import java.time.Instant

@deprecated("Replaced with BufferedOllamaResponse", "v1.4")
object BufferedReply
{
	// COMPUTED -----------------------
	
	/**
	  * @return An empty reply
	  */
	def empty = apply("", "", OllamaResponseStatistics.empty, Now)
	
	
	// OTHER    -----------------------
	
	/**
	  * Converts an Ollama API -originated response model into a buffered reply.
	  * Should only be called for models where "done" is set to true (i.e. for the final responses).
	  * @param responseModel A response model returned by the Ollama API
	  * @return A buffered reply (final) parsed from the response
	  */
	def fromOllamaResponse(responseModel: Model) = {
		val (text, think) = ReplyParseUtils.separateThinkFrom(responseModel("response").getString)
		apply(text, think,
			OllamaResponseStatistics.fromOllamaResponse(responseModel), responseModel("created_at").getInstant)
	}
}

/**
  * A reply from an LLM that has been completely read
  * @param text Text contents of this reply
  * @param thoughts Reflective content produced by the LLM before giving the final answer
  * @param statistics Statistics concerning this reply
  * @param lastUpdated Origin time of this reply
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  */
@deprecated("Replaced with BufferedOllamaResponse", "v1.4")
case class BufferedReply(text: String, thoughts: String, statistics: OllamaResponseStatistics, lastUpdated: Instant = Now)
	extends BufferedOllamaResponse with BufferedOllamaResponseLike[BufferedReply] with Reply
{
	// IMPLEMENTED  -----------------------
	
	override def self: BufferedReply = this
}
