package utopia.metropolis.model.error

/**
  * These exceptions are thrown when a resource has already been used and cannot be replicated
  * @author Mikko Hilpinen
  * @since 2.5.2020, v2
  */
class AlreadyUsedException(message: String) extends Exception(message)
