package utopia.echo.model.llm

import utopia.echo.model.unit.ByteCount

/**
 * Contains/determines how much VRAM should be reserved for a specific LLM
 * @param modelSize VRAM required by the LLM (static)
 * @param kiloTokenCost VRAM required for a 1024 token request cache (input + output)
 * @author Mikko Hilpinen
 * @since 05.03.2026, v1.5
 */
case class LlmVramUse(modelSize: ByteCount, kiloTokenCost: ByteCount)
