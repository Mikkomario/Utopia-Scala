package utopia.echo.model.response

import utopia.annex.model.manifest.HasSchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, Final}
import utopia.flow.time.Now

import java.time.Instant

object BufferedResponse
{
	// OTHER    ------------------------
	
	/**
	 * @param text Text contents of this response
	 * @param tokenUsage Statistics about token usage
	 * @param state Whether this response was a success or a failure
	 * @param created Time when this (version of this) response was created
	 * @return A new response
	 */
	def apply(text: String, tokenUsage: TokenUsage, state: Final = Alive, created: Instant = Now): BufferedResponse =
		_BufferedResponse(text, tokenUsage, state, created)
	
	
	// NESTED   ------------------------
	
	private case class _BufferedResponse(text: String, tokenUsage: TokenUsage, state: Final, lastUpdated: Instant)
		extends BufferedResponse
}

/**
 * Common trait for buffered responses, regardless of service provider
 * @author Mikko Hilpinen
 * @since 02.09.2025, v1.4
 */
trait BufferedResponse extends HasSchrodingerState
{
	/**
	 * @return Text contents of this response. Usually not including reflection / thinking content.
	 */
	def text: String
	/**
	 * @return Time when the latest version of this response was originated.
	 */
	def lastUpdated: Instant
	/**
	 * @return Statistics about token usage in this interaction
	 */
	def tokenUsage: TokenUsage
	/**
	 * @return Whether this response was a success or a failure
	 */
	def state: Final
}