package utopia.echo.model.settings

import utopia.echo.model.enumeration.ModelParameter
import utopia.echo.model.enumeration.ModelParameter.{ContextTokens, PredictTokens}
import utopia.echo.model.tokenization.PartiallyEstimatedTokenCount
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.view.immutable.caching.Lazy

import scala.util.{Failure, Success}

/**
 * Common trait for instances which specify context size limits
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
trait HasContextSizeLimits
{
	// ABSTRACT -----------------------------
	
	/**
	 * @return Limits applied to context size
	 */
	def contextSizeLimits: ContextSizeLimits
	
	/**
	 * @return Whether thinking output is expected
	 */
	def thinks: Boolean
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return Minimum applied context size
	 */
	def minContextSize = if (thinks) contextSizeLimits.minWithThink else contextSizeLimits.min
	/**
	 * @return Maximum applied context size
	 */
	def maxContextSize: Int = contextSizeLimits.max
	
	/**
	 * @return Additional context size applied always when possible (may be limited by [[maxContextSize]])
	 */
	def additionalContextSize: Int  = contextSizeLimits.additional
	
	
	// OTHER    ----------------------------
	
	/**
	 * Applies context size limits to model settings,
	 * ensuring that [[ContextTokens]] and [[PredictTokens]] are both specified.
	 * Won't override existing parameters.
	 * @param settings Settings to modify
	 * @param messages Outgoing messages
	 * @param expectedReplySize Expected size of the received reply, in tokens
	 * @param expectedThinkSize Expected size of the received think output, in tokens (default = 0)
	 * @param history Size of the existing conversation history (default = 0)
	 * @return Model settings to apply. Failure if no tokens could be reserved for the response
	 *         (i.e. when maximum context size is reached by the prompt alone).
	 *         Also returns the calculated token count inside a lazy wrapper
	 *         (no token counts are calculated when context size is already specified in the 'settings').
	 */
	def applyLimitsTo(settings: ModelSettings, messages: => IterableOnce[String],
	                  expectedReplySize: => Int, expectedThinkSize: => Int = 0,
	                  history: => PartiallyEstimatedTokenCount = PartiallyEstimatedTokenCount.zero) =
	{
		val customReplySize = settings.get(PredictTokens).int
		val lazyTokenCounts = Lazy {
			customReplySize match {
				case Some(customPredictLimit) => contextSizeFor(messages, customPredictLimit, history = history)
				case None => contextSizeFor(messages, expectedReplySize, expectedThinkSize, history)
			}
		}
		val modifiedSettings = settings.get(ContextTokens).int match {
			// Case: Custom context size specified
			case Some(customContextSize) =>
				// Case: Custom max response size also specified => Won't modify settings
				if (customReplySize.isDefined)
					Success(settings)
				else {
					val tokenCounts = lazyTokenCounts.value
					val contextSizeReduction = (tokenCounts.context - customContextSize) max 0
					// Case: The custom context size is too small to fit any response => Fails
					if (contextSizeReduction >= tokenCounts.maxResponse)
						Failure(new IllegalArgumentException(
							s"Specified context size of $customContextSize is too small to contain this request"))
					// TODO: Make the safety margin customizable
					else
						Success(settings + (PredictTokens -> (tokenCounts.maxResponse - contextSizeReduction)))
				}
			// Case: No custom context size => Assigns a context size based on other calculations
			case None =>
				val tokenCounts = lazyTokenCounts.value
				if (tokenCounts.maxResponse > 0) {
					// Case: Custom max response size specified => Won't override it
					if (customReplySize.isDefined)
						Success(settings + (ContextTokens -> tokenCounts.context))
					else
						Success(settings ++ Pair[(ModelParameter, Value)](
							ContextTokens -> tokenCounts.context, PredictTokens -> tokenCounts.maxResponse))
				}
				// Case: Context size can't be increased enough to fit a response (max context size exceeded) => Fails
				else
					Failure(new IllegalStateException("Context size is not large enough to fit this request"))
		}
		modifiedSettings -> lazyTokenCounts
	}
	
	/**
	 * Calculates context size to reserve for a specific query
	 * @param messages Outgoing messages / prompt
	 * @param expectedReplySize Expected size of the reply, in tokens
	 * @param expectedThinkSize Expected size of the reply's think section, if applicable, in tokens. Default = 0.
	 * @param history Size of the applied conversation history. Default = 0.
	 * @return Context size that should be reserved.
	 */
	def contextSizeFor(messages: IterableOnce[String], expectedReplySize: Int, expectedThinkSize: Int = 0,
	                   history: PartiallyEstimatedTokenCount = PartiallyEstimatedTokenCount.zero) =
		contextSizeLimits.contextSizeFor(messages, expectedReplySize, expectedThinkSize, history, thinks)
}
