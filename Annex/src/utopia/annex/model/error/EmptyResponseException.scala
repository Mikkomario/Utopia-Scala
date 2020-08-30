package utopia.annex.model.error

/**
  * Thrown when response content is expected but an empty response is received instead
  * @author Mikko Hilpinen
  * @since 11.7.2020, v1
  */
class EmptyResponseException(message: String) extends RequestFailedException(message)
