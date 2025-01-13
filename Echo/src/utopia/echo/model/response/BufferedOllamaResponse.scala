package utopia.echo.model.response

/**
 * Common trait for buffered ollama responses.
 * @author Mikko Hilpinen
 * @since 12.01.2025, v1.2
 */
trait BufferedOllamaResponse extends OllamaResponse with BufferedOllamaResponseLike[BufferedOllamaResponse]
