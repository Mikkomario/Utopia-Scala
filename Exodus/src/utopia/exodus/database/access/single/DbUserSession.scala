package utopia.exodus.database.access.single

import java.time.Instant
import java.util.UUID.randomUUID

import utopia.exodus.database.access.many.DbUserSessions
import utopia.exodus.database.factory.user.SessionFactory
import utopia.exodus.database.model.user.SessionModel
import utopia.exodus.model.partial.UserSessionData
import utopia.exodus.model.stored.UserSession
import utopia.flow.util.TimeExtensions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleModelAccess, UniqueAccess}

/**
  * Used for accessing individual user sessions in DB
  * @author Mikko Hilpinen
  * @since 3.5.2020, v2
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
	  * @param deviceId Id of targeted device
	  * @return An access point to the user's session on the specified device
	  */
	def apply(userId: Int, deviceId: Int) = new SingleDeviceSession(userId, deviceId)
	
	/**
	  * @param sessionKey A session key
	  * @param connection DB Connection (implicit)
	  * @return An active session matching specified session key
	  */
	def matching(sessionKey: String)(implicit connection: Connection) =
		find(model.withKey(sessionKey).toCondition)
	
	
	// NESTED	----------------------------------
	
	class SingleDeviceSession(userId: Int, deviceId: Int) extends UniqueAccess[UserSession]
		with SingleModelAccess[UserSession]
	{
		// ATTRIBUTES	---------------------------
		
		private val targetingCondition = model.withUserId(userId).withDeviceId(deviceId).toCondition
		
		
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
		  * @param connection DB Connection (implicit)
		  * @return New user session
		  */
		def start()(implicit connection: Connection) =
		{
			// Before starting a new session, makes sure to terminate existing user sessions for this device
			DbUserSessions.forDeviceWithId(deviceId).end()
			// Creates a new session that lasts for 24 hours or until logged out
			model.insert(UserSessionData(userId, deviceId, randomUUID().toString, Instant.now() + 24.hours))
		}
	}
}
