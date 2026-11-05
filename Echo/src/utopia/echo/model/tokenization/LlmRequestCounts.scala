package utopia.echo.model.tokenization

import utopia.echo.model.response.TokenUsage
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.{MayBeZero, Reversible}
import utopia.flow.operator.combine.Combinable.SelfCombinable

object LlmRequestCounts
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
 * @param confirmedTokens Number of tokens in the completed requests
 * @author Mikko Hilpinen
 * @since 04.03.2026, v1.5
 */
case class LlmRequestCounts(requests: Int, completed: Int, failures: Int, confirmedTokens: TokenUsage)
	extends MayBeZero[LlmRequestCounts] with SelfCombinable[LlmRequestCounts] with Reversible[LlmRequestCounts]
		with ModelConvertible
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
	
	@deprecated("Deprecated for removal. Renamed to .confirmedTokens", "v1.6")
	def tokens = confirmedTokens
	
	
	// IMPLEMENTED  -------------------
	
	override def self: LlmRequestCounts = this
	override def zero: LlmRequestCounts = LlmRequestCounts.zero
	override def isZero: Boolean = requests == 0
	
	override def unary_- : LlmRequestCounts = LlmRequestCounts(-requests, -completed, -failures, -confirmedTokens)
	
	override def toModel: Model = Model.from("requests" -> requests, "completed" -> completed,
		"failures" -> failures, "tokens" -> confirmedTokens)
	
	override def +(other: LlmRequestCounts): LlmRequestCounts =
		LlmRequestCounts(requests + other.requests, completed + other.completed, failures + other.failures,
			confirmedTokens + other.confirmedTokens)
}