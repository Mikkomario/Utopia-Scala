package utopia.echo.model.response

import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.LinearScalable
import utopia.flow.util.NumberExtensions._

object TokenUsage
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * A zero token usage instance (0 tokens in, 0 tokens out)
	 */
	lazy val zero = apply(0, 0)
	
	
	// OTHER    --------------------------
	
	/**
	 * @param input The number of tokens used in the prompt
	 * @param output The number of tokens used for generating the response, including reasoning tokens
	 * @return Token usage based on the specified input
	 */
	def apply(input: Int, output: Int): TokenUsage = _TokenUsage(input, output)
	
	
	// NESTED   --------------------------
	
	private case class _TokenUsage(input: Int, output: Int) extends TokenUsage
	{
		override def total: Int = input + output
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
	def input: Int
	/**
	 * @return The number of tokens used for generating the response, including reasoning tokens
	 */
	def output: Int
	/**
	 * @return Total number of tokens used throughout the request
	 */
	def total: Int
	
	
	// IMPLEMENTED  -------------------
	
	override def self: TokenUsage = this
	
	override def toString: String = s"${ tokensStr(input) } + ${ tokensStr(output) } = ${ tokensStr(total) }"
	
	override def +(other: TokenUsage): TokenUsage = TokenUsage(input + other.input, output + other.output)
	override def *(mod: Double): TokenUsage = TokenUsage((input * mod).round.toInt, (output * mod).round.toInt)
	
	
	// OTHER    ----------------------
	
	private def tokensStr(tokens: Int) = {
		if (tokens > 1000000)
			s"${ (tokens / 1000000.0).roundDecimals(1) } M"
		else if (tokens > 10000)
			s"${ tokens / 1000 } K"
		else if (tokens > 1000)
			s"${ (tokens / 1000.0).roundDecimals(1) } K"
		else
			tokens.toString
	}
}
