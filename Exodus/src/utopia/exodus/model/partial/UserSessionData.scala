package utopia.exodus.model.partial

import java.time.Instant

/**
  * Contains basic information about a temporary user session
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  * @param userId Id of the user who owns this session key
  * @param key The session key
  * @param expires Time threshold when this key is no longer usable
  * @param deviceId Id of the device that uses this session key. None if this session is not for any specific
  *                 device (browser quest mode) (default)
  */
case class UserSessionData(userId: Int, key: String, expires: Instant, deviceId: Option[Int] = None)
