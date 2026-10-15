package utopia.flow.error

/**
 * Type of exceptions thrown when cyclical references are being iterated.
 * @author Mikko Hilpinen
 * @since 14.10.2026, v2.9
 */
class CyclicIterationException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)
