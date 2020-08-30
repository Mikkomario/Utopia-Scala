package utopia.annex.model.error

/**
  * A more generic exception used when a request failed or an unexpected response status was received
  * @author Mikko Hilpinen
  * @since 11.7.2020, v1
  */
class RequestFailedException(message: String) extends Exception(message)
