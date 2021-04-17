package utopia.journey.model.error

/**
  * An error thrown when user data is not available
  * @author Mikko Hilpinen
  * @since 18.7.2020, v0.1
  */
class NoUserDataError(message: String) extends Exception(message)
