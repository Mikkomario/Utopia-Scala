package utopia.echo.model.error

/**
 * Used for signaling LLM reply parsing failures
 * @author Mikko Hilpinen
 * @since 23.07.2025, v1.4
 */
class ParseException(message: String, cause: Throwable = null) extends Exception(message, cause)
