package utopia.echo.model.llm

import utopia.echo.model.tokenization.{RequestCount, TokenCount}
import utopia.echo.model.unit.ByteCount

/**
 * Contains/determines how much VRAM should be reserved for a specific LLM
 * @param modelSize VRAM required by the LLM / hosting system setup
 *                  (static, the remaining VRAM is assumed to be available for the KV-cache)
 * @param kiloTokenCost VRAM required for a 1000 token request cache (input + output)
 * @param maxContextSize The absolute maximum context size supported by this model
 * @param requestBufferSize Amount of additional VRAM required per request, regardless of request size
 * @param requestTokenRounding Upwards rounding that should be applied to request token counts.
 *                             Default = None = do not round.
 * @author Mikko Hilpinen
 * @since 05.03.2026, v1.5
 */
case class LlmVramUse(modelSize: ByteCount, kiloTokenCost: ByteCount, maxContextSize: TokenCount,
                      requestBufferSize: ByteCount = ByteCount.zero, requestTokenRounding: Option[TokenCount] = None)
{
	/**
	 * @param vram Available VRAM
	 * @return Maximum context size for that VRAM amount
	 */
	def maxContextSizeOn(vram: ByteCount) =
		floorTokens(TokenCount(((vram - modelSize - requestBufferSize) / kiloTokenCost * 1000).toInt)) min
			maxContextSize
	
	/**
	 * @param tokens Number of tokens in a single request (input + estimated output)
	 * @return VRAM required to run that request
	 */
	def inOneRequest(tokens: TokenCount) = in(RequestCount.unit(tokens))
	/**
	 * @param requests Information about n requests
	 * @return VRAM required to run that number of requests in parallel
	 */
	def in(requests: RequestCount) =
		requestBufferSize * requests.requests + kiloTokenCost * (ceilTokens(requests.tokens).value / 1000.0)
	
	/**
	 * Rounds a token count up, applying [[requestTokenRounding]], if applicable
	 * @param tokens Token count to round up
	 * @return Rounded token count
	 */
	def ceilTokens(tokens: TokenCount) = requestTokenRounding match {
		case Some(rounding) => rounding * (tokens.value.toDouble / rounding.value).ceil.toInt
		case None => tokens
	}
	/**
	 * Rounds a token count down, applying [[requestTokenRounding]], if applicable
	 * @param tokens Token count to round down
	 * @return Rounded token count
	 */
	def floorTokens(tokens: TokenCount) = requestTokenRounding match {
		case Some(rounding) => rounding * (tokens.value.toDouble / rounding.value).floor.toInt
		case None => tokens
	}
}