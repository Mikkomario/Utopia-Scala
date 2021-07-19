package utopia.ambassador.model.error

/**
  * Thrown when (service) settings can't be found in a context where they are required
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
class SettingsNotFoundException(message: String, cause: Throwable = null) extends Exception(message, cause)
