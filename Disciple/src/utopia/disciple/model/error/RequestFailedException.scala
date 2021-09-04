package utopia.disciple.model.error

/**
  * An exception thrown when a request fails
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.4.2
  */
class RequestFailedException(message: String, cause: Throwable = null) extends Exception(message, cause)
