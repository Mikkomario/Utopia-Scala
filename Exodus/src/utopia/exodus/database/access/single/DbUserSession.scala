package utopia.exodus.database.access.single

import utopia.exodus.database.access.many.DbUserSessions
import utopia.exodus.database.factory.user.SessionFactory
import utopia.exodus.database.model.user.SessionModel
import utopia.exodus.model.partial.UserSessionData
import utopia.exodus.model.stored.UserSession
import utopia.exodus.util.UuidGenerator
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleModelAccess, UniqueModelAccess}

/**
  * Used for accessing individual user sessions in DB
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  */
object DbUserSession extends SingleModelAccess[UserSession]
{
	// IMPLEMENTED	-----------------------------
	
	override def factory = SessionFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	---------------------------------
	
	private def model = SessionModel
	
	
	// OTHER	---------------------------------
	
	/**
	  * @param userId Id of targeted user
	  * @param deviceId Id of targeted device (None if not targeting any specific device)
	  * @return An access point to the user's session on the specified device
	  */
	def apply(userId: Int, deviceId: Option[Int]) = new SingleDeviceSession(userId, deviceId)
	
	/**
	  * @param userId Id of targeted user
	  * @param deviceId Id of targeted device
	  * @return An access point to the user's session on the specified device
	  */
	def apply(userId: Int, deviceId: Int): SingleDeviceSession = apply(userId, Some(deviceId))
	
	/**
	  * @param userId Id of targeted user
	  * @return An access point to the user's session that's not connected to any device
	  */
	def deviceless(userId: Int) = apply(userId, None)
	
	/**
	  * @param sessionKey A session key
	  * @param connection DB Connection (implicit)
	  * @return An active session matching specified session key
	  */
	def matching(sessionKey: String)(implicit connection: Connection) =
		find(model.withKey(sessionKey).toCondition)
	
	
	// NESTED	----------------------------------
	
	class SingleDeviceSession(userId: Int, deviceId: Option[Int]) extends UniqueModelAccess[UserSession]
	{
		// ATTRIBUTES	---------------------------
		
		private val targetingCondition =
		{
			// Targets either session on a device or the user's deviceless session
			val base = model.withUserId(userId)
			deviceId match
			{
				case Some(deviceId) => base.withDeviceId(deviceId).toCondition
				case None => base.toCondition && model.deviceIdColumn.isNull
			}
		}
		
		
		// IMPLEMENTED	---------------------------
		
		override def condition = DbUserSession.mergeCondition(targetingCondition)
		
		override def factory = DbUserSession.factory
		
		
		// OTHER	-------------------------------
		
		/**
		  * Ends this user session (= logs the user out from this device)
		  * @param connection DB Connection (implicit)
		  * @return Whether any change was made
		  */
		def end()(implicit connection: Connection) =
		{
			// Deprecates existing active session
			model.nowLoggedOut.updateWhere(condition) > 0
		}
		
		/**
		  * Starts a new session on this device. Logs out any previous user(s) of this device as well.
		  * @param preferredModelStyle Model style preferred to be used during this session (optional)
		  * @param connection DB Connection (implicit)
		  * @return New user session
		  */
		def start(preferredModelStyle: Option[ModelStyle] = None)
		         (implicit connection: Connection, uuidGenerator: UuidGenerator) =
		{
			// Before starting a new session, makes sure to terminate existing user sessions for this device
			// On deviceless sessions, terminates the previous deviceless session
			deviceId match
			{
				case Some(deviceId) => DbUserSessions.forDeviceWithId(deviceId).end()
				case None => end()
			}
			// Creates a new session that lasts for 24 hours or until logged out
			model.insert(UserSessionData(userId, uuidGenerator.next(), Now + 24.hours, deviceId, preferredModelStyle))
		}
	}
}
