package utopia.echo.controller

import utopia.echo.model.tokenization.EstimatedTokenCount
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
object EstimateTokenCount
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
	
	
	// OTHER    ----------------------------
	
	/**
	  * Estimates the number of tokens within some text
	  * @param text A string
	  * @return Estimated number of tokens within that text.
	 *         Contains both the raw (heuristic) estimate, and a corrected value based on historical results.
	  */
	// Counts the number of words
	def in(text: String) = {
		val raw = _in(text)
		val corrected = (raw * correctionModPointer.value).round.toInt
		// Handles the edge case where correction would set the token count to 0
		EstimatedTokenCount(raw, if (corrected == 0 && text.nonEmpty) 1 else corrected)
	}
	
	/**
	 * @param text A string
	 * @return A pointer that contains an up-to-date estimate of the specified string's token count.
	 *         Updated as feedback is given to this interface.
	 */
	def continuallyIn(text: String) = {
		lazy val raw = _in(text)
		correctionModPointer.map { EstimatedTokenCount.withCorrectionMod(raw, _) }
	}
	/**
	 * @param messages A sequence of messages
	 * @return A pointer that contains an up-to-date estimate of the combined token count in the specified messages.
	 *         Updated as feedback is given to this interface.
	 */
	def continuallyIn(messages: IterableOnce[String]) = {
		lazy val raw = messages.iterator.map(_in).sum
		correctionModPointer.map { EstimatedTokenCount.withCorrectionMod(raw, _) }
	}
	
	/**
	 * Provides feedback for this interface, so that future estimations may be more accurate
	 * @param rawEstimate The "raw" token count estimated by this interface
	 * @param actualCount The actual number of tokens reported by the LLM
	 */
	def feedback(rawEstimate: Int, actualCount: Int) =
		historyP.update { _ + EstimatedTokenCount(rawEstimate, actualCount) }
	/**
	 * Helps this interface become more accurate by providing training sample.
	 * An alternative for [[feedback]].
	 * @param text A text
	 * @param correctTokenCount Token count in the specified text
	 */
	def train(text: String, correctTokenCount: Int) = feedback(_in(text), correctTokenCount)
		
	private def _in(text: String) = text.splitIterator(Regex.whiteSpace)
		.map { word =>
			// Each word may consist of multiple parts (e.g. "it's" or "e-commerce")
			word.trim.splitIterator(specialCharSeqRegex)
				.map { part =>
					// The first 4 letters count as a single token
					// After that, every 3 letters count as a token
					if (part.length >= 9)
						(part.length / 3) min 5
					else if (part.length > 4)
						2
					else
						1
				}
				.sum
		}
		.sum
}
