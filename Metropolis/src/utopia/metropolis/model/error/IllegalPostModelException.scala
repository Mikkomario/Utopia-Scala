package utopia.metropolis.model.error

/**
  * Thrown when posted model data doesn't contain all information or some information is invalid
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
class IllegalPostModelException(message: String) extends Exception(message)
