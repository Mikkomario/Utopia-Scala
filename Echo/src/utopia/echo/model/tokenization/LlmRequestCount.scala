package utopia.echo.model.tokenization

import utopia.echo.model.response.TokenUsage
import utopia.flow.operator.{MayBeZero, Reversible}
import utopia.flow.operator.combine.Combinable.SelfCombinable

object LlmRequestCount
{
	/**
	 * A count where everything is 0
	 */
	lazy val zero = apply(0, 0, 0, TokenUsage.zero)
}

/**
 * Represents a counts of requests performed, pending and failed, as well as token usage.
 * @param requests Number of requests performed (including those currently being executed)
 * @param completed Number of requests completed (including failures)
 * @param failures Number of currently pending requests
 * @param tokens Number of tokens in the completed requests
 * @author Mikko Hilpinen
 * @since 04.03.2026, v1.5
 */
case class LlmRequestCount(requests: Int, completed: Int, failures: Int, tokens: TokenUsage)
	extends MayBeZero[LlmRequestCount] with SelfCombinable[LlmRequestCount] with Reversible[LlmRequestCount]
{
	// COMPUTED -----------------------
	
	/**
	 * @return Number of currently pending requests
	 */
	def pending = requests - completed
	
	/**
	 * @return Ratio of failed requests
	 */
	def failureRatio = if (completed == 0) 0.0 else failures.toDouble / completed
	
	
	// IMPLEMENTED  -------------------
	
	override def self: LlmRequestCount = this
	override def zero: LlmRequestCount = LlmRequestCount.zero
	override def isZero: Boolean = requests == 0
	
	override def unary_- : LlmRequestCount = LlmRequestCount(-requests, -completed, -failures, -tokens)
	
	override def +(other: LlmRequestCount): LlmRequestCount =
		LlmRequestCount(requests + other.requests, completed + other.completed, failures + other.failures,
			tokens + other.tokens)
}