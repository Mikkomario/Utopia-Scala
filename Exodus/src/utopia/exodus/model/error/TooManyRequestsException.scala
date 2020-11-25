package utopia.exodus.model.error

/**
  * An exception thrown when there are too many requests being performed in general or from a certain source
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
class TooManyRequestsException(message: String) extends Exception(message)
