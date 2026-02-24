package utopia.echo.model.request.ollama

/**
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
package object chat
{
	@deprecated("Renamed to OllamaChatRequest", "v1.4.1")
	type ChatRequest[+R] = OllamaChatRequest[R]
	@deprecated("Renamed to BufferedOllamaChatRequest", "v1.4.1")
	type BufferedChatRequest = BufferedOllamaChatRequest
}
