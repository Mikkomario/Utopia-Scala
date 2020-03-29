package utopia.disciple.http

/**
  * Thrown when trying to read an entity without content
  * @author Mikko Hilpinen
  * @since 20.7.2019, v1.1+
  */
class NoContentException(message: String) extends RuntimeException(message)
