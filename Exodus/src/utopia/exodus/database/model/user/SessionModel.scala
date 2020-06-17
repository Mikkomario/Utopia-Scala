package utopia.exodus.database.model.user

import java.time.Instant

import utopia.exodus.database.factory.user.SessionFactory
import utopia.exodus.model.partial.UserSessionData
import utopia.exodus.model.stored.UserSession
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object SessionModel
{
	// COMPUTED	-----------------------------------
	
	/**
	  * @return A new model that has just been marked as logged out
	  */
	def nowLoggedOut = apply(logoutTime = Some(Instant.now()))
	
	
	// OTHER	-----------------------------------
	
	/**
	  * @param userId Id of the targeted user
	  * @return A model with only user id set
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
	
	/**
	  * @param deviceId Id of the targeted device
	  * @return A model with only device id set
	  */
	def withDeviceId(deviceId: Int) = apply(deviceId = Some(deviceId))
	
	/**
	  * @param key Session key
	  * @return A model with only key set
	  */
	def withKey(key: String) = apply(key = Some(key))
	
	/**
	  * @param expireTime Session key expiration timestamp
	  * @return A model with only expiration time set
	  */
	def expiringIn(expireTime: Instant) = apply(expires = Some(expireTime))
	
	/**
	  * Inserts a new user session to DB
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted session
	  */
	def insert(data: UserSessionData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(data.userId), Some(data.deviceId), Some(data.key), Some(data.expires)).insert().getInt
		UserSession(newId, data)
	}
}

/**
  * Used for interacting with user session data in DB
  * @author Mikko Hilpinen
  * @since 3.5.2020, v2
  */
case class SessionModel(id: Option[Int] = None, userId: Option[Int] = None, deviceId: Option[Int] = None,
						key: Option[String] = None, expires: Option[Instant] = None, logoutTime: Option[Instant] = None)
	extends StorableWithFactory[UserSession]
{
	// IMPLEMENTED	-------------------------------
	
	override def factory = SessionFactory
	
	override def valueProperties = Vector("id" -> id, "userId" -> userId, "deviceId" -> deviceId, "key" -> key,
		"expiresIn" -> expires, "logoutTime" -> logoutTime)
	
	
	// OTHER	------------------------------------
	
	/**
	  * @param deviceId Id of targeted device
	  * @return A copy of this model with specified device id
	  */
	def withDeviceId(deviceId: Int) = copy(deviceId = Some(deviceId))
}
