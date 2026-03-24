package utopia.echo.controller.tokenization

import utopia.echo.model.tokenization.{EstimatedTokenCount, TokenCount}
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.SysErrLogger
import utopia.flow.view.mutable.Pointer

/**
  * A simplistic interface for estimating the number of tokens within strings.
  * This logic assumes that each word matches about 1-3 tokens, where each token is at least 3 characters long.
  * @author Mikko Hilpinen
  * @since 19.09.2024, v1.1
  */
object EstimateTokenCount extends TokenCounter
{
	// ATTRIBUTES   ------------------------
	
	private val specialCharSeqRegex = (!Regex.letterOrDigit).withinParentheses.oneOrMoreTimes
	
	/**
	 * A pointer that contains historical raw estimates,
	 * coupled with the matching correct token counts returned by the LLMs.
	 * Used for calculating the estimate correction factor.
	 */
	// Starts with a positive count in order to reduce the effects of the first feedback entries
	private val historyP = Pointer.eventful(EstimatedTokenCount(400, 400))(SysErrLogger)
	/**
	 * Contains the modifier to apply to all (future) token estimates in order to match the LLM's tokenization logic.
	 */
	val correctionModPointer = historyP.map { count => if (count.isZero) 1.0 else count.corrected / count.raw.toDouble }
	
	
	// IMPLEMENTED  ------------------------
	
	override def tokensIn(text: String): EstimatedTokenCount = {
		if (text.isEmpty)
			EstimatedTokenCount.zero
		else {
			val raw = _in(text)
			val corrected = (raw * correctionModPointer.value).ceil.toInt
			// Handles the edge case where correction would set the token count to 0
			EstimatedTokenCount(raw, if (corrected == 0) 1 else corrected)
		}
	}
	
	/**
	 * Provides feedback for this interface, so that future estimations may be more accurate
	 * @param rawEstimate The "raw" token count estimated by this interface
	 * @param actualCount The actual number of tokens reported by the LLM
	 */
	override def feedback(rawEstimate: Int, actualCount: TokenCount) =
		historyP.update { _ + EstimatedTokenCount(rawEstimate, actualCount.value) }
	
	
	// OTHER    ----------------------------
	
	@deprecated("Renamed to .tokensIn(String)", "v1.6")
	def in(text: String) = tokensIn(text)
	
	@deprecated("Renamed to .countContinuallyFrom(String)", "v1.6")
	def continuallyIn(text: String) = countContinuallyFrom(text)
	@deprecated("Renamed to .countContinuallyFrom(IterableOnce)", "v1.6")
	def continuallyIn(messages: IterableOnce[String]) = countContinuallyFrom(messages)
		
	private def _in(text: String) = text.splitIterator(Regex.whitespace)
		.map { word =>
			// Each word may consist of multiple parts (e.g. "it's" or "e-commerce")
			specialCharSeqRegex.divide(word.trim).iterator
				.map {
					case Left(text) =>
						// The first 4 letters count as a single token
						// After that, every 3 letters count as a token
						if (text.length >= 9)
							(text.length / 3) min 5
						else if (text.length > 4)
							2
						else
							1
					// Case: A separator consisting of special characters => These are also counted as 1-2 tokens
					case Right(separator) =>
						if (separator.length < 4)
							1
						else
							2
				}
				.sum
		}
		.sum
}
