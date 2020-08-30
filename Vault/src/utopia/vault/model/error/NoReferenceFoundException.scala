package utopia.vault.model.error

/**
  * Thrown when a required reference is missing
  * @author Mikko Hilpinen
  * @since 7.8.2020, v1.6
  */
class NoReferenceFoundException(message: String) extends RuntimeException(message)
