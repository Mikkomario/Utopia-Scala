package utopia.echo.model.llm

import utopia.echo.model.tokenization.{RequestCount, TokenCount}
import utopia.echo.model.unit.ByteCount

/**
 * Contains/determines how much VRAM should be reserved for a specific LLM
 * @param modelSize VRAM required by the LLM (static)
 * @param kiloTokenCost VRAM required for a 1000 token request cache (input + output)
 * @param maxContextSize The absolute maximum context size supported by this model
 * @param requestBufferSize Amount of VRAM required per request, regardless of request size
 * @author Mikko Hilpinen
 * @since 05.03.2026, v1.5
 */
case class LlmVramUse(modelSize: ByteCount, kiloTokenCost: ByteCount, maxContextSize: TokenCount,
                      requestBufferSize: ByteCount = ByteCount.zero)
{
	/**
	 * @param vram Available VRAM
	 * @return Maximum context size for that VRAM amount
	 */
	def maxContextSizeOn(vram: ByteCount) =
		TokenCount(((vram - modelSize - requestBufferSize) / kiloTokenCost * 1000).toInt) min maxContextSize
	
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
		requestBufferSize * requests.requests + kiloTokenCost * (requests.tokens.value / 1000.0)
}