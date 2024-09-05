package utopia.echo.model.response

/**
 * Common trait for Ollama's text-based responses.
 * This trait removes generic typing, being useful in situations where the specific (Repr) types are not important.
 * @author Mikko Hilpinen
 * @since 04.09.2024, v1.1
 */
trait OllamaResponse extends OllamaResponseLike[OllamaResponse]
