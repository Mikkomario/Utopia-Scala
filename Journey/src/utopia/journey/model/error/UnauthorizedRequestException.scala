package utopia.journey.model.error

/**
  * Thrown when a request couldn't be fulfilled because of missing, invalid or expired authorization
  * @author Mikko Hilpinen
  * @since 11.7.2020, v1
  */
class UnauthorizedRequestException(message: String) extends RequestDeniedException(message)
