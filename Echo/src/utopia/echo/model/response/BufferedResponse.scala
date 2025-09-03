package utopia.echo.model.response

import utopia.flow.time.Now

import java.time.Instant

object BufferedResponse
{
	// OTHER    ------------------------
	
	/**
	 * @param text Text contents of this response
	 * @param thoughts The reflective / reasoning content produced by the LLM before the final answer.
	 * @param tokenUsage Statistics about token usage
	 * @param created Time when this (version of this) response was created
	 * @return A new response
	 */
	def apply(text: String, thoughts: String = "", tokenUsage: TokenUsage = TokenUsage.zero,
	          created: Instant = Now): BufferedResponse =
		_BufferedResponse(text, thoughts, tokenUsage, created)
	
	
	// NESTED   ------------------------
	
	private case class _BufferedResponse(text: String, thoughts: String, tokenUsage: TokenUsage, lastUpdated: Instant)
		extends BufferedResponse
}

/**
 * Common trait for buffered responses, regardless of service provider
 * @author Mikko Hilpinen
 * @since 02.09.2025, v1.4
 */
trait BufferedResponse
{
	/**
	 * @return Text contents of this response. Usually not including reflection / thinking content.
	 */
	def text: String
	/**
	 * @return The reflective / reasoning content produced by the LLM before the final answer.
	 *         May be empty.
	 */
	def thoughts: String
	/**
	 * @return Time when the latest version of this response was originated.
	 */
	def lastUpdated: Instant
	/**
	 * @return Statistics about token usage in this interaction
	 */
	def tokenUsage: TokenUsage
}