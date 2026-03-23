package utopia.echo.model.response

import utopia.echo.model.tokenization.TokenCount
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.LinearScalable
import utopia.flow.util.NumberExtensions._

object TokenUsage
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * A zero token usage instance (0 tokens in, 0 tokens out)
	 */
	lazy val zero = apply(TokenCount.zero, TokenCount.zero)
	
	
	// OTHER    --------------------------
	
	/**
	 * @param input The number of tokens used in the prompt
	 * @param output The number of tokens used for generating the response, including reasoning tokens
	 * @return Token usage based on the specified input
	 */
	def apply(input: TokenCount, output: TokenCount): TokenUsage = _TokenUsage(input, output)
	
	
	// NESTED   --------------------------
	
	private case class _TokenUsage(input: TokenCount, output: TokenCount) extends TokenUsage
	{
		override def total: TokenCount = input + output
	}
}

/**
 * Common trait for statistics instances that contain general request token usages
 * @author Mikko Hilpinen
 * @since 02.09.2025, v1.4
 */
trait TokenUsage extends SelfCombinable[TokenUsage] with LinearScalable[TokenUsage]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return The number of tokens used in the prompt
	 */
	def input: TokenCount
	/**
	 * @return The number of tokens used for generating the response, including reasoning tokens
	 */
	def output: TokenCount
	/**
	 * @return Total number of tokens used throughout the request
	 */
	def total: TokenCount
	
	
	// IMPLEMENTED  -------------------
	
	override def self: TokenUsage = this
	
	override def toString: String = s"$input + $output = $total"
	
	override def +(other: TokenUsage): TokenUsage = TokenUsage(input + other.input, output + other.output)
	override def *(mod: Double): TokenUsage = TokenUsage(input * mod, output * mod)
}
