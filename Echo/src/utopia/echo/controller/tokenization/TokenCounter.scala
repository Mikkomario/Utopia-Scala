package utopia.echo.controller.tokenization

import utopia.echo.model.tokenization.{EstimatedTokenCount, TokenCount}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

/**
  * Common trait for token counting logic implementations
  * @author Mikko Hilpinen
  * @since 24.03.2026, v1.6
  */
trait TokenCounter
{
	// ABSTRACT ----------------------------
	
	/**
	 * Contains the modifier to apply to all (future) token estimates in order to match the LLM's tokenization logic.
	 */
	def correctionModPointer: Changing[Double]
	
	/**
	 * Estimates the number of tokens within some text
	 * @param text A string
	 * @return Estimated number of tokens within that text.
	 *         Contains both the raw (heuristic) estimate, and a corrected value based on historical results.
	 */
	def tokensIn(text: String): EstimatedTokenCount
	
	/**
	 * Provides feedback for this interface, so that future estimations may be more accurate
	 * @param rawEstimate The "raw" token count estimated by this interface
	 * @param actualCount The actual number of tokens reported by the LLM
	 */
	def feedback(rawEstimate: Int, actualCount: TokenCount): Unit
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param messages A sequence of messages
	 * @return Total number of tokens used in the specified messages
	 */
	def tokensIn(messages: IterableOnce[String]): EstimatedTokenCount = messages.nonEmptyIterator match {
		case Some(messagesIter) => messagesIter.map(tokensIn).reduce { _ + _ }
		case None => EstimatedTokenCount.zero
	}
	
	/**
	 * @param text A string
	 * @return A pointer that contains an up-to-date estimate of the specified string's token count.
	 *         Updated as feedback is given to this interface.
	 */
	def countContinuallyFrom(text: String) = {
		if (text.isEmpty)
			Fixed(EstimatedTokenCount.zero)
		else {
			val raw = tokensIn(text)
			correctionModPointer.map(raw.withCorrectionModifier)
		}
	}
	/**
	 * @param messages A sequence of messages
	 * @return A pointer that contains an up-to-date estimate of the combined token count in the specified messages.
	 *         Updated as feedback is given to this interface.
	 */
	def countContinuallyFrom(messages: IterableOnce[String]) = {
		messages.nonEmptyIterator match {
			case Some(messagesIter) =>
				val raw = messagesIter.map(tokensIn).reduce { _ + _ }
				correctionModPointer.map(raw.withCorrectionModifier)
			case None => Fixed(EstimatedTokenCount.zero)
		}
	}
	
	/**
	 * Helps this interface become more accurate by providing training sample.
	 * An alternative for [[feedback]].
	 * @param text A text
	 * @param correctTokenCount Token count in the specified text
	 */
	def train(text: String, correctTokenCount: TokenCount) = feedback(tokensIn(text).raw, correctTokenCount)
}
