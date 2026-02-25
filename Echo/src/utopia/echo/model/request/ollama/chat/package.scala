package utopia.echo.model.request.ollama

/**
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
package object chat
{
	@deprecated("Renamed to OllamaChatRequest", "v1.5")
	type ChatRequest[+R] = OllamaChatRequest[R]
	@deprecated("Renamed to BufferedOllamaChatRequest", "v1.5")
	type BufferedChatRequest = BufferedOllamaChatRequest
}
