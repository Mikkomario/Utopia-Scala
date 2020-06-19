package utopia.exodus.model.partial

/**
  * Contains basic information about a device authentication key
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  * @param userId Id of the key owner
  * @param deviceId Id of the device this key is tied to
  * @param key Unique key
  */
case class DeviceKeyData(userId: Int, deviceId: Int, key: String)
