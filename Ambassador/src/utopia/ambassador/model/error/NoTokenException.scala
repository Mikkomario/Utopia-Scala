package utopia.ambassador.model.error

/**
  * Thrown when there is no valid access token available for the required function
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
class NoTokenException(message: String, cause: Throwable = null) extends Exception(message, cause)
