package utopia.echo.model.response.ollama

import utopia.echo.util.ReplyParseUtils
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now

import java.time.Instant

object BufferedOllamaResponse
{
	// COMPUTED -----------------------
	
	/**
	 * @return An empty response
	 */
	def empty = apply("", "", OllamaResponseStatistics.empty, Now)
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param text Reply text
	 * @param thoughts Reply thinking content
	 * @param statistics Reply statistics
	 * @param created Time when this (final version of) reply was created
	 * @return A new response containing the specified info
	 */
	def apply(text: String, thoughts: String, statistics: OllamaResponseStatistics,
	          created: Instant = Now): BufferedOllamaResponse =
		_BufferedOllamaResponse(text, thoughts, statistics, created)
	
	/**
	 * Converts an Ollama API -originated /generate endpoint response model into a buffered response.
	 * Should only be called for models where "done" is set to true (i.e. for the final responses).
	 * @param responseModel A response model returned by the Ollama API
	 * @return A buffered response (final) parsed from the response
	 */
	def fromOllamaGenerateResponse(responseModel: Model) = {
		val (text, think) = ReplyParseUtils.separateThinkFrom(responseModel("response").getString)
		apply(text, think,
			OllamaResponseStatistics.fromOllamaResponse(responseModel), responseModel("created_at").getInstant)
	}
	
	
	// NESTED   ----------------------------
	
	private case class _BufferedOllamaResponse(text: String, thoughts: String, statistics: OllamaResponseStatistics,
	                                           lastUpdated: Instant)
		extends BufferedOllamaResponse
	{
		// IMPLEMENTED  -----------------------
		
		override def self: _BufferedOllamaResponse = this
	}
}

/**
 * Common trait for buffered ollama responses.
 * @author Mikko Hilpinen
 * @since 12.01.2025, v1.2
 */
trait BufferedOllamaResponse extends OllamaResponse with BufferedOllamaResponseLike[BufferedOllamaResponse]
