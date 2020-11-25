package utopia.exodus.model.error

/**
  * Thrown when a token or a key is invalid, missing or expired
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
class InvalidKeyException(message: String) extends Exception(message)
