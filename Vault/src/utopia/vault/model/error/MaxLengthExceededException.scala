package utopia.vault.model.error

/**
  * Thrown when column (or other) maximum length is exceeded
  * @author Mikko Hilpinen
  * @since 21.11.2021, v1.12
  */
class MaxLengthExceededException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)
