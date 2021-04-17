package utopia.journey.model

/**
  * Information the user provides on login
  * @author Mikko Hilpinen
  * @since 20.6.2020, v0.1
  * @param email User email address
  * @param password User password
  * @param allowDeviceKeyUse Whether the user wishes to skip login in future and use a static device key instead
  *                          (default = false)
  */
case class UserCredentials(email: String, password: String, allowDeviceKeyUse: Boolean = false)
