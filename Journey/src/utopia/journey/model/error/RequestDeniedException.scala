package utopia.journey.model.error

/**
  * Thrown when a request was denied by the server for some reason
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  */
class RequestDeniedException(message: String) extends RequestFailedException(message)
