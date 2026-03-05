package utopia.echo.model.settings

import utopia.echo.controller.EstimateTokenCount
import utopia.echo.model.tokenization.{EstimatedTokenCount, PartiallyEstimatedTokenCount, TokenCounts}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.util.Mutate

import scala.util.{Failure, Success, Try}

object ContextSizeLimits extends FromModelFactory[ContextSizeLimits]
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * Default context size limits
	 */
	lazy val default = apply()
	
	
	// IMPLEMENTED  -----------------------
	
	override def apply(model: HasProperties): Try[ContextSizeLimits] =
		model
			.tryGet("max") { value =>
				value.int match {
					case Some(max) =>
						if (max <= 0)
							Failure(new IllegalArgumentException("Maximum context size must be positive"))
						else
							Success(max)
					case None => Success(8192)
				}
			}
			.map { max => apply(max, model("additional"), model("min"), model("minWithThink")) }
}

/**
 * Settings used for determining how much context size is reserved for messaging
 * @param max Absolute maximum context size, in tokens. Default = 8192.
 *            If the conversation context becomes larger than this value, the LLM may not apply it fully.
 *            Larger values may lead to larger (V-RAM) memory use.
 * @param additional Number of tokens reserved in addition to the expected reply size,
 *                              for safeguard / buffering. Default = 256.
 * @param min Smallest used context size. Default = 1024.
 * @param minWithThink Smallest used context size when thinking mode is enabled. Default = 2048.
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class ContextSizeLimits(max: Int = 8192, additional: Int = 256, min: Int = 1024, minWithThink: Int = 2048)
	extends ModelConvertible
{
	// IMPLEMENTED  -----------------------
	
	override def toModel: Model = Model.from("min" -> min, "max" -> max, "additional" -> additional,
		"minWithThink" -> minWithThink)
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param min New minimum context size
	 * @return Copy of these limits with the specified absolute minimum
	 */
	def withMin(min: Int) = copy(min = min, minWithThink = minWithThink max min, max = max.max(min))
	def mapMin(f: Mutate[Int]) = withMin(f(min))
	/**
	 * @param min Minimum context size to use in thinking mode
	 * @return Copy of these limits with the specified conditional minimum
	 */
	def withMinWhenThinking(min: Int) =
		copy(minWithThink = min, min = this.min min min, max = max.max(min))
	def mapMinWhenThinking(f: Mutate[Int]) = withMinWhenThinking(f(minWithThink))
	
	/**
	 * @param max Absolute maximum context size
	 * @return Copy of these limits with the specified maximum
	 */
	def withMax(max: Int) = copy(max = max, min = min.min(max))
	def mapMax(f: Mutate[Int]) = withMax(f(max))
	
	/**
	 * @param additional Additional context size reserved for every request, if possible
	 * @return Copy of these limits with the specified additional size
	 */
	def withAdditional(additional: Int) = copy(additional = additional)
	def mapAdditional(f: Mutate[Int]) = withAdditional(f(additional))
	
	/**
	 * Estimates the context size required for a query
	 * @param messages Messages to send out as the prompt. Not including messages in 'history'.
	 * @param expectedReplySize Expected size of the reply, not including thinking tokens
	 * @param expectedThinkSize Expected size of the thinking output. Default = 0.
	 * @param history Size of the conversation history. Default = 0.
	 * @param thinks Whether thinking output is expected. Default = false.
	 * @return Token counts to use for that query.
	 */
	def contextSizeFor(messages: IterableOnce[String], expectedReplySize: Int, expectedThinkSize: Int = 0,
	                   history: PartiallyEstimatedTokenCount = PartiallyEstimatedTokenCount.zero,
	                   thinks: Boolean = false) =
	{
		// Estimates the number of tokens in the message and in the reply
		val messageSize = messages.nonEmptyIterator match {
			case Some(iterator) => iterator.map(EstimateTokenCount.in).reduce { _ + _ }
			case None => EstimatedTokenCount.zero
		}
		val total = {
			// Attempts to estimate the reply size. Limits the result to the current limits.
			val raw = history.corrected + messageSize.corrected + expectedReplySize + additional
			val includingThink = if (thinks) raw + expectedThinkSize else raw
			
			if (includingThink >= max)
				max
			else if (thinks && includingThink < minWithThink)
				minWithThink
			else if (includingThink <= min)
				min
			else
				includingThink
		}
		TokenCounts(messageSize, history, total)
	}
}