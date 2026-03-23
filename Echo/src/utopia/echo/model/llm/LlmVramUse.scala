package utopia.echo.model.llm

import utopia.echo.model.tokenization.TokenCount
import utopia.echo.model.unit.ByteCount

/**
 * Contains/determines how much VRAM should be reserved for a specific LLM
 * @param modelSize VRAM required by the LLM (static)
 * @param kiloTokenCost VRAM required for a 1000 token request cache (input + output)
 * @author Mikko Hilpinen
 * @since 05.03.2026, v1.5
 */
case class LlmVramUse(modelSize: ByteCount, kiloTokenCost: ByteCount)
{
	/**
	 * @param vram Available VRAM
	 * @return Maximum context size for that VRAM amount
	 */
	def maxContextSizeOn(vram: ByteCount) = TokenCount(((vram - modelSize) / kiloTokenCost * 1000).toInt)
}