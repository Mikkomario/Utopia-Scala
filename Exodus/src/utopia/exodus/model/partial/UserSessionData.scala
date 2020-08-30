package utopia.exodus.model.partial

import java.time.Instant

/**
  * Contains basic information about a temporary user session
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  * @param userId Id of the user who owns this session key
  * @param deviceId Id of the device that uses this session key
  * @param key The session key
  * @param expires Time threshold when this key is no longer usable
  */
case class UserSessionData(userId: Int, deviceId: Int, key: String, expires: Instant)
