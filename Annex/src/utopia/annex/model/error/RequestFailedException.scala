package utopia.annex.model.error

/**
  * A more generic exception used when a request failed or an unexpected response status was received
  * @author Mikko Hilpinen
  * @since 11.7.2020, v1
  */
@deprecated("Please use the new exception class with the same name in Disciple instead", "v1.0.2")
class RequestFailedException(message: String) extends Exception(message)
